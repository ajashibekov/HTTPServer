import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.util.zip.GZIPOutputStream;

public class ProcessingWorker implements Runnable {
    private Socket socket = null;
	private boolean chunked = true;

    public ProcessingWorker(Socket socket){
        this.socket = socket;
    }

	private String contentType(String filename){
		if(filename.endsWith(".html")) return "text/html";
		else if (filename.endsWith(".css")) return "text/css";
		else if (filename.endsWith(".js")) return "application/javascript";
		else if (filename.endsWith(".jpg")) return "image/jpeg";
		else if (filename.endsWith(".png")) return "image/png";
		else if (filename.endsWith(".pdf")) return "application/pdf";
		else return "*/*";
	}

    public void run(){
		try (
            BufferedReader in = new BufferedReader(
                new InputStreamReader(
                    socket.getInputStream()));
        ){
			Vector<String> requestHeaders = new Vector<String>(); 
			int line_count = 0;
			boolean gzip = false;
			String inputLine = in.readLine();
	
			while(inputLine != null && inputLine.trim().length() != 0){
				requestHeaders.addElement(inputLine); 
				if(inputLine.contains("gzip")) gzip = true;
				inputLine = in.readLine();
				line_count++;
			}
			
			if(requestHeaders.size() == 0) return; 
			
			String requestLine = requestHeaders.elementAt(0);
			String[] parts = requestLine.split(" ");
			String path = parts[1];
			String prot = parts[2]; 			
			
			if(path.equals("/")) {path = "/index.html";}
			
			String resp = prot + " ";
			boolean fileExists = (new File(path.substring(1)).isFile());
			
			if(fileExists){
				resp += "200 OK\r\n";
			}
			else {
				resp += "404 Not Found\r\n";
				path = "/404.html";
			}
			
			String content_type = contentType(path.substring(1));
			
			//gzip = false;
			if(gzip) {
				// Compress the file, change the file path to a different file
				try {
					FileInputStream file_inp_stream = new FileInputStream(path.substring(1));
					FileOutputStream file_out_stream = new FileOutputStream(path.substring(1) + ".gz");
					GZIPOutputStream gzip_stream = new GZIPOutputStream(file_out_stream);
					
					byte[] buffer = new byte[1024];
					int len;
					while((len = file_inp_stream.read(buffer)) != -1){
						gzip_stream.write(buffer, 0, len);
					}

					gzip_stream.close();
					file_inp_stream.close();
					file_out_stream.close();
					
				} catch (IOException e) {
					// do nothing
				}
				path += ".gz";
			}
			
			String file = new String(Files.readAllBytes(Paths.get(path.substring(1))));
			
			resp += "Content-Type: " + content_type + "\r\n";
			if(!chunked)
				resp += "Content-Length: " + file.length() + "\r\n";
			else
				resp += "Transfer-Encoding: chunked\r\n";
			
			if(gzip)
				resp += "Content-Encoding: gzip\r\n";
			resp += "Connection: close\r\n\r\n";
			
			BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
			
			out.write(resp.getBytes());
			try {
				byte[] buf = new byte [512];
				int len = 0;
				FileInputStream fsm = new FileInputStream(path.substring(1));
				while((len = fsm.read(buf)) != -1){
					if(chunked){
						String hex = Integer.toHexString(len);
						hex += "\r\n";
						out.write(hex.getBytes());
					}
					out.write(buf, 0, len);
					if(chunked){
						out.write("\r\n".getBytes());
						out.flush();
					}
				}

				if(chunked)
					out.write("0\r\n\r\n".getBytes());
				
				out.flush();
				fsm.close();
				out.close();
			} catch (IOException e) {
				// do nothing
			}
            socket.close();
			
        } catch (IOException e) {
            // do nothing
        }
		
	}
}