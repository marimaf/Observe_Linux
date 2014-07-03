import java.io.IOException;


public class Main {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		
		//Ruta a archivo de configuración
		String path_config = "./observe.config";
		
		//Creo instancia de observe
		Observe observe = new Observe(path_config);
		
		//Comienzo a escuchar
		observe.Run();
		
	}

}
