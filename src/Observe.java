import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;



public class Observe {

	private String out_file = "./out.cap";
	private String log_file = "./log.txt";
	private String Server_IP;
	private String Server_PORT;
	private String Branch_id;
	private String Tablet_id;
	private int Scan_delta;
	private String Scan_time;
	
	public Observe(String config_path) throws IOException
	{
		//Cargo configuración
		BufferedReader in = new BufferedReader(new FileReader(config_path));
		String l;
		while((l = in.readLine())!= null)
		{
			String[] param = l.split("\t");
			switch(param[0])
			{
				case "Server_IP":
					Server_IP = param[1];
					break;
				case "Server_PORT":
					Server_PORT = param[1];
					break;
				case "Branch_id":
					Branch_id = param[1];
					break;
				case "Tablet_id":
					Tablet_id = param[1];
					break;
				case "Scan_delta":
					Scan_delta = Integer.parseInt(param[1]);
					break;
				case "Scan_time":
					Scan_time = param[1];
					break;
				default:
					break;
			}
		}
		
		in.close();
		
		System.out.println(Server_IP);
		System.out.println(Server_PORT);
		System.out.println(Branch_id);
		System.out.println(Tablet_id);
		System.out.println(Scan_delta);
		System.out.println(Scan_time);
	}
	
	public void Run() throws InterruptedException, IOException
	{
		//Llamado a consola para matar mon0 si existe (airmon-ng stop mon0)
		kill_mon0();
		
		//Llamada a consola para iniciar mon0 (airmon-ng start wlan0)
		start_mon0();

		try
		{
			while(true)
			{
				//Escucho datos (timeout 10 tcpdump -i mon0 -I -y IEEE802_11_RADIO -s 1500 -w example.cap)
				System.out.println("Scanning...");
				capture_packages();
				//Envío datos
				System.out.println("Sending...");
				send_cap();
				
				//Espero hasta volver a capturar datos
				System.out.println("Waiting...");
				Thread.sleep(Scan_delta*1000);
			}	
		}
		catch(Exception e)
		{
			e.printStackTrace();
			//Fin! mato mon0
			// airmon-ng stop mon0
			kill_mon0();	
		}
	}
	
	/***
	 * Envío el archivo cap al servidor. 
	 * @throws IOException 
	 */
	private void send_cap() throws IOException
	{
		//Ruta al servidor
		String request = Server_IP + ":" + Server_PORT + "/histories/upload.json";

		//Conexión post
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost method = new HttpPost(request);
        MultipartEntity entity = new MultipartEntity();
        
        //Parámetros
        entity.addPart("tablet[tp_id_branch]", new StringBody(Branch_id, Charset.forName("UTF-8")));
        entity.addPart("tablet[tp_id_tablet]", new StringBody(Tablet_id, Charset.forName("UTF-8")));
        entity.addPart("cap", new FileBody(new File(out_file)));
        entity.addPart("datetime", new StringBody(datetime_now(), Charset.forName("UTF-8")));
        method.setEntity(entity);
        
        //Envío request
        HttpResponse responseBody = httpclient.execute(method);
        System.out.println(responseBody.getStatusLine());

	}

	/**
	 * Retorna la fecha en formato datetime de ruby
	 * @return
	 */
	private String datetime_now()
	{
		Calendar c = Calendar.getInstance();
		
		// Objetivo => 2014-06-23T00:31:55-04:00
		SimpleDateFormat formatter_date=new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat formatter_time=new SimpleDateFormat("HH:mm:ss");
		SimpleDateFormat formatter_zone=new SimpleDateFormat("Z");
		
		String date =formatter_date.format(c.getTime());
		String time =formatter_time.format(c.getTime()); 
		String zone =formatter_zone.format(c.getTime()); 
		//Ajusto zona horaria
		String r = "([+,-])([0-9][0-9])([0-9][0-9])";
		zone = zone.replaceAll(r, "$1$2:$3");
		String datetime = date + "T" + time + zone;
		
		return datetime;
	}
	
	private void show_log() throws IOException
	{
		//Muestro la fecha
		Date d = new Date();
		System.out.println(d.toString());
		
		BufferedReader in = new BufferedReader(new FileReader(log_file));
		String l;
		while((l = in.readLine())!= null)
			System.out.println(l);
		in.close();
	}
	
	private void capture_packages() throws InterruptedException, IOException
	{
		String[] cmd = {"timeout", Scan_time, "tcpdump", "-i", "mon0", "-I", "-y", "IEEE802_11_RADIO", "-s", "1500", "-w", out_file};
		run_in_console(cmd);
		show_log();
	}
	
	private void kill_mon0() throws InterruptedException, IOException
	{
		String[] cmd = {"airmon-ng", "stop", "mon0"};
		run_in_console(cmd);
	}
	
	private void start_mon0() throws InterruptedException, IOException
	{
		String[] cmd = {"airmon-ng", "start", "wlan0"};
		run_in_console(cmd);
	}
	
	private void run_in_console(String[] cmd) throws InterruptedException, IOException
	{
		File output = new File("./log.txt");

		Process p = new ProcessBuilder(cmd).redirectError(Redirect.INHERIT).redirectOutput(Redirect.to(output)).start();
		p.waitFor();
	}

}
