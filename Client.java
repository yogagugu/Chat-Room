import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client extends Socket{
	
	private static final int TIME_OUT = 30 * 60 * 1000;//TIME_OUT is measured in millisecond, i.e 30min=30*60*1000milliseconds
	PrintWriter printWriter =new PrintWriter(this.getOutputStream(),true);//output to server
    BufferedReader bufferedReader =new BufferedReader(new InputStreamReader(this.getInputStream()));//input from server
    boolean exit;//indicate whether client logged out
	
	public Client(String ip, int port) throws Exception{
		super(ip,port);
		this.setSoTimeout(TIME_OUT);
		new SocketClientThread(this); //Create new thread
        
	}
	  
	
	class SocketClientThread extends Thread{
		
		Socket client;
		public SocketClientThread(Socket client) throws IOException{
			this.client = client ;
			start();
		}

//		login connection with server
		public void run(){
			try {
	            Scanner input;
	            input = new Scanner(System.in);
	            while(true){
	            	String temp;
		            System.out.println("Input your username:");
		            printWriter.println(input.next());
		            printWriter.flush();
		            temp = bufferedReader.readLine();
		            System.out.println(temp);
		            if(temp.equals("user has logged in or blocked")){
		            	continue;
		            }
		            if(temp.equals("user does not exist")){
		            	continue;
		            }
		            System.out.println("Input your password:");
		            printWriter.println(input.next());
		            printWriter.flush();
		            temp = bufferedReader.readLine();
		            System.out.println(temp);
	            	if(temp.equals("Welcome to simple chat server!")){
	            		break;
		            }
	            	if(temp.equals("password is incorrect!")){
	            		continue;
		            }
	            	if(temp.equals("user will be blocked")){
		            	continue;
		            }
	            }
	            
	            class MessageThread extends Thread{
	            	boolean stopThread ;
	            	public MessageThread() {
	                    stopThread = false;
	                    start();
	                }
// exit	         
	        		public void run(){
	        			String result = "";
	    	            
	        			try {
	        				while(result.indexOf("bye") == -1){
							result = bufferedReader.readLine();
							System.out.println(result);
	        				}
	        				System.out.println("If program didn't exit, press Enter key!");
	        				exit = false;
						} catch (IOException e) {
							e.printStackTrace();
							System.out.println("Something wrong happened, press Enter key to exit!");
							exit = false;
						}
	        		}
	        	}
	            MessageThread msgThread = new MessageThread();
	            
	            exit = true;
	            while(exit){
	            	BufferedReader sysBuff =new BufferedReader(new InputStreamReader(System.in));
		            printWriter.println(sysBuff.readLine());
		            printWriter.flush();
	            }    
	            
	            msgThread.stopThread = true;
	            while(!msgThread.stopThread){};
	            
	            System.out.println("exit!");
	            printWriter.close();
	            bufferedReader.close();
	            client.close();
	            input.close();
	            
	        }catch (IOException e) {
	            System.out.println("Exception:" + e);
	        }
	    }
		}
	
//main method	
    public static void main(String[] args) {
    	try{
    		Client socket =new Client(args[0],Integer.parseInt(args[1]));
    	}
    	catch(Exception e){
    		System.out.println("Exception:" + e);
    	}
    }
}
