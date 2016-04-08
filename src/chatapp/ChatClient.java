/*
    Group 58:
    Michael Irizarry (miriza6@uic.edu)
    Wenkan Zhu (wzhu24@uic.edu)

    CS 342 - Project 4
    Networked Chat Application
 */

package chatapp;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import javax.swing.*;

public class ChatClient 
{
    private final ClientEvent frontend;
    private String username = "";
    ServerConnection server = null;
    private boolean connected = false;
    
    public ChatClient(ClientEvent frontend)
    {
        this.frontend = frontend;
    }
    
    public boolean connected()
    {
    	return connected;
    }
    
    public void start(String ip, int portNumber)
    {
        if(connected) return;
    	
    	try
        {
            server = new ServerConnection(ip, portNumber);
            frontend.connectServer(ip, portNumber);
            connected = true;
        }
        catch (IOException ex)
        {
            frontend.chatClientError("Failed to connect to server; " + ex.getMessage());
            Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }
    
    public void stop()
    {
        if(!connected) return;
    	
    	try
        {
            server.disconnect();
            frontend.disconnectServer();
            connected = false;
        }
        catch (IOException ex)
        {
            frontend.chatClientError("Failed to stop server; " + ex.getMessage());
            Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }
    
    public void setUserName(String name)
    {
        username = name;
        if(connected) server.changeUserName(username);
    }
    
    public String getUserName()
    {
        return username;
    }
    
    public void sendMessage(String message)
    {
        if(server != null)
    	
        {
            try
            {
                server.sendPacket(ClientPacket.constructPublicMessagePacket(message));
            	
                frontend.sendMessageToAll(message);
            }
            catch (IOException ex)
            {
                frontend.chatClientError("Failed to send message; " + ex.getMessage());
                Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void sendMessage(String username, String message)
    {
        if(server != null)
    	
        {
            try
            {
                server.sendPacket(ClientPacket.constructPrivateMessagePacket(username, message));
                
                frontend.sendMessageToUser(username, message);
            }
            catch (IOException ex)
            {
                frontend.chatClientError("Failed to send message; " + ex.getMessage());
                Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private class ServerConnection extends Thread implements ClientPacket
    {
        private Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        boolean active = false;
        
        public ServerConnection(String IP, int port) throws IOException
        {
            super();
            socket = new Socket(IP, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            active = true;
            changeUserName(username);
            super.start();
        }
        
        public void disconnect() throws IOException
        {
            out.writeObject("OFFLINE");
        	
        	try
            {
                in.close();
                out.close();
                socket.close();
            }
            finally
            {
                active = false;
            }
        }
        
        @Override
        public void run()
        {
            try
            {
                String packet;
                while((packet = (String)in.readObject()) != null)
                {
                    ClientPacket.processPacket(this, packet);
                }
                
                in.close();
                out.close();
                socket.close();
            }
            catch (EOFException ex)
            {
            	active = false;
            	connected = false;
            	
            	frontend.chatClientError("Server offline.");
            }
            catch (SocketException ex)
        	{
        		
        	}
            catch (IOException ex)
            {
                Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
            }
            catch (ClassNotFoundException ex)
            {
                Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        public void changeUserName(String username)
        {
        	try 
        	{
				out.writeObject(ClientPacket.constructUserNamePacket(username));
			} 
        	catch (IOException e) 
        	{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        @Override
        public void setUserList(List<String> userlist)
        {
            frontend.receiveUserList(userlist);
        }

        @Override
        public void receivePublicMessage(String senderName, String message)
        {
            if(!username.equals(senderName)) frontend.receiveMessage(senderName, message, false);
        }

        @Override
        public void receivePrivateMessage(String username, String message)
        {
            frontend.receiveMessage(username, message, true);
        }

        @Override
        public void receiveStatusPacket(String status, boolean isError)
        {
            frontend.receiveStatusPacket(status, isError);
        }

        @Override
        public void packetError(String packet, String error)
        {
            frontend.chatClientError("PACKET ERROR (" + error + "): " + packet);
        }
        
        private void sendPacket(String packet) throws IOException
        {
            out.writeObject(packet);
        }
    }
    
}
