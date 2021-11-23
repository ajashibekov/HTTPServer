import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;

public class HttpServer {
	
	private static int port_number = 9988;
	private static int num_clients = 0;
	
	public static void main(String args[]) throws IOException{
		ServerSocket server_socket = new ServerSocket(port_number);
		System.out.println("Listening to connections on port " + port_number);
		while(true){
			Socket client_socket = server_socket.accept();
			System.out.println("Client " + num_clients + " joined");
			new Thread(new ProcessingWorker(client_socket)).start();
			num_clients++;
		}
	}
	
}