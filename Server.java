import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
  
public class Server extends ServerSocket {
    private static final ReadUser readUser = new ReadUser();
    private static final int BLOCK_TIME = 60;//count in second,i.e if block time=60s,BLOCK_TIME=60 
    private static final long LAST_LOGIN_TIME =  60*60*1000;//count in millisecond,i.e 1h=60*60*1000milliseconds;
    private boolean [] isUserLogin = new boolean [readUser.username.length];//indicate whether a user logged in
    private long[] userLoginTime = new long[readUser.username.length];//time user logged in
    @SuppressWarnings("unchecked") 
    private LinkedList<String>[] userMsgList = new LinkedList [readUser.username.length];//store a list of message with regard to every user
    
    public Server(int SERVER_PORT)throws IOException {
        super(SERVER_PORT);
        for(int i = 0; i<isUserLogin.length; i++){
        	isUserLogin[i] = false;
        	userLoginTime[i] = -1;//initialization
        }
        for(int i = 0; i<readUser.username.length;i++){
        	userMsgList[i] = new LinkedList<String>();// create user message list for a new user
        }
        try {
            while (true) {
                Socket socket = this.accept();
                new CreateServerThread(socket);// create new thread
            }
        }catch (IOException e) {
        }finally {
            close();
        }
    }

// Return a list of online users
    String WhoElse(int except){
    	String loginUserlist = "";
    	for(int i = 0; i<isUserLogin.length; i++){
        	if(isUserLogin[i] && i!= except){//Make sure one's own name will not display on his/her screen
        		loginUserlist += readUser.username[i]+" ";
        	}
        }
    	return loginUserlist;
    }
    
// Return a list of users who are online within last one hour
    String Wholasthr(int except){
    	String loginUserlist = "";
    	long now = System.currentTimeMillis();
    	for(int i = 0; i<isUserLogin.length; i++){
    		if(i != except){
	        	if(  isUserLogin[i] || ( (userLoginTime[i] != -1)&&(now-userLoginTime[i]<LAST_LOGIN_TIME) )  ) {
	        		loginUserlist += readUser.username[i]+" ";
	        	}
    		}
        }
    	return loginUserlist;
    }
    
// Block IP for 60 seconds
    public class MyTask extends TimerTask{
    	int curUserNum;
        MyTask(int curUserNum){
        	this.curUserNum = curUserNum;
        }
        public void run(){
        	isUserLogin[curUserNum] = false;
        }
    }
    
    class CreateServerThread extends Thread {
    	
        private Socket client;
        private BufferedReader bufferedReader;
        private PrintWriter printWriter;
        int curUserNum;
// Initialization before creating thread
        public CreateServerThread(Socket s)throws IOException {
            client = s;
            bufferedReader =new BufferedReader(new InputStreamReader(client.getInputStream()));
            printWriter =new PrintWriter(client.getOutputStream(),true);
            
            start();
        }

// count login number        
        public void run() {
            try {
        		int loginCounter = 1;
        		String lastName = "";
        		String clientIp = client.getLocalAddress().getHostAddress(); 
        		String curClientName = getName();
        		String name ;

            	while(true){
            		name = bufferedReader.readLine();
// Check if a user has logged in or doesn't exist
	            	if((curUserNum = readUser.getUserNum(name))!= -1 ){
		            	boolean curIpLogin = isUserLogin[curUserNum];
		            	if(curIpLogin){
		            		printWriter.println("user has logged in or blocked");
		            		continue;
		            	}
	            	}
	            	else{
	            		printWriter.println("user does not exist");
	            		continue;
	            	}
	            	printWriter.println("user has input");//username is valid
	            	printWriter.flush();
	            	System.out.println(name);
	            	
	            	String pass = bufferedReader.readLine();
	            	System.out.println(pass);
	            	
	            	if(!readUser.checkUser(name, pass)){
	            		if(lastName.equals(name)){
	            			loginCounter++;	//when username is the same as last time,count the number of trials
	            		}
	            		else{
	            			loginCounter = 1;//when username is different from last time, start all over again
	            			lastName = name;
	            		}
	            		if(loginCounter >= 3){//when same username fails three times
	            			printWriter.println("user will be blocked");
		            		printWriter.flush();
		            		System.out.println(clientIp+" "+curClientName+loginCounter+"consecutive password failures，block IP for"+BLOCK_TIME+"s!");
		            		isUserLogin[curUserNum] = true;
// Timer for block
		                	MyTask myTask = new MyTask(curUserNum);
		                    Timer timer = new Timer();
		                    timer.schedule(myTask, BLOCK_TIME*1000);
		                    loginCounter = 0;//Clear up the counter for the previous blocked username
	            		}
	            		else{
	            			printWriter.println("password is incorrect!");
		            		printWriter.flush();
		            		System.out.println(clientIp+" "+curClientName+"Invalid password!");
	            		}
	            	}
	            	else{
		            	printWriter.println("Welcome to simple chat server!");
		        		printWriter.flush();
		        		System.out.println(clientIp+" "+curClientName+"logged in!");
		        		isUserLogin[curUserNum] = true;
		        		break;
	            	}
	            		
            	}
            	
            	class MessageThread extends Thread{
	            	boolean stopThread ;
	            	int curUserNum;
	            	public MessageThread(int curUserNum) {
	                    stopThread = true;
	                    this.curUserNum = curUserNum;
	                    start();
	                }
// read user's message list	        		
	            	public void run(){
	        			while(stopThread){
	        				System.out.print("");
        					while(!userMsgList[curUserNum].isEmpty()){
        						String temp = userMsgList[curUserNum].removeLast();//if there is message,get it from tail
        						int num = temp.indexOf(",");
	                        	printWriter.println(temp.substring(0, num)+": "+ temp.substring(num+1, temp.length()));
	        		        	printWriter.flush();
	        		        	
                        	}
        				}
	        		}
	        	}
	            MessageThread msgThread = new MessageThread(curUserNum);
            	
            	String line;
                boolean exit = false;
                ParseString passStr = new ParseString();
                while(!exit){
                	
                	line = bufferedReader.readLine();
                	System.out.println(line);
                	String t1;
            		String [] t2;
// implement logout                	
            		if(line.equals("logout")){
                		exit = true;
                		printWriter.println("bye");
		        		printWriter.flush();
		        		isUserLogin[curUserNum] = false;
		        		userLoginTime[curUserNum] = System.currentTimeMillis();//Record time user logged out
		        		continue;
                	}	
// implement whoelse
                	if(line.equals("whoelse")){
                		printWriter.println(WhoElse(curUserNum));
                		printWriter.flush();
                		continue;
                	}
// implement wholasthr
                	if(line.equals("wholasthr")){
                		printWriter.println(Wholasthr(curUserNum));
                		printWriter.flush();
                		continue;
                	}
// implement broadcast(including offline broadcast)                	
            		if((t1 = passStr.parseBroadcast(line))!= null){
            			printWriter.println("You want to say \"" + t1 +"\" to everyone!");
                		printWriter.flush();
                		for(int i = 0;i<userMsgList.length;i++){
                			userMsgList[i].addFirst(name+","+t1);//make sure the output message is in correct order
                		}
                		continue;
            		}
// implement message(including offline message)             
		        	if((t2 = passStr.parseMessage(line))!= null){
                		if(readUser.getUserNum(t2[0])!= -1){// if message sent to a valid user
	                		userMsgList[readUser.getUserNum(t2[0])].addFirst(name+","+t2[1]);
	                		printWriter.println("You want to say \"" + t2[1] +"\" to \""+t2[0]+"\"!");
	                    	printWriter.flush();
	                    	continue;
                			}
                		else{
                			printWriter.println("the user you input \""+t2[0]+"\" does not exist!");
        		        	printWriter.flush();
	                    	continue;
                			}
                		}
// unrecognized command		        
                		printWriter.println( curClientName +",the command doesn't exist!");
                		printWriter.flush();
                	}
                
                msgThread.stopThread = false;
                
                System.out.println("Client(" + curClientName +") exit!");
                printWriter.close();
                bufferedReader.close();
                client.close();
            }catch (IOException e) {
            	isUserLogin[curUserNum] = false;
            	userLoginTime[curUserNum] = System.currentTimeMillis();
            	System.out.println("IOException!");
                printWriter.close();
            }
            
        }
    }

//  main method    
    public static void main(String[] args)throws IOException {
    	
    	Server server = new Server(Integer.parseInt(args[0]));//open a server socket to listen
        
    }
}
