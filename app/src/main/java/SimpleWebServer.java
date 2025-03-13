
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mysql.cj.x.protobuf.MysqlxConnection.CapabilitiesSetOrBuilder;

class SimpleWebServer {

	String url = "jdbc:mysql://dim47.es:3307/"; // URL de conexión
	String usuario = "root"; // usuario de MySQL
	String contraseña = "4321"; // contraseña de MySQL
	String query = "";

	int puerto = 0;
	static String raizWeb = null;
	static DB db = null;

	SimpleWebServer(int puerto, String raizWeb) {
		this.puerto = puerto; // Puerto en el que el servidor escuchará
		this.raizWeb = raizWeb;
	}

	void arrancar() {
		try {
			ServerSocket serverSocket = new ServerSocket(puerto);
			System.out.println("Servidor web escuchando en el puerto " + puerto);

			db = new DB("jdbc:mysql://dim47.es:3307/sanchezVentas", "root", "4321");
			db.conectar();
			while (true) {
				Socket clientSocket = serverSocket.accept();
				analizarRequest(clientSocket);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void analizarRequest(Socket clientSocket) throws IOException {
		int opcion = 0;
		int res = 0;
		String query = "";
		String html = "";
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
			String cabeceraHTTP = in.readLine();
			if (cabeceraHTTP != null) {
				String[] requestParts = cabeceraHTTP.split(" ");
				if (requestParts.length == 3 && requestParts[0].equals("GET")) {
					String ficheroHTMLpedido = requestParts[1];
					Map<String, String> params = obtenerParametros(ficheroHTMLpedido);

					String[] orden = ficheroHTMLpedido.split("/");
					if (ficheroHTMLpedido == null || ficheroHTMLpedido.equals("") || ficheroHTMLpedido.equals("/")) {
						servirFicheroHTML("/index.html", out);

					} else if (ficheroHTMLpedido.startsWith("/accionDQL")) {
						if (params.isEmpty()) {
							sinParámetros(out);

						} else {
							String op = params.get("opcion");
							opcion = 0;
							try {
								opcion = Integer.parseInt(op);
							} catch (NumberFormatException e) {
								opcion = 0;
							}
							if (opcion < 100) {
								lanzarDQL_responderHTM(opcion, out);
							} else {
								responderHTM_Formulario(opcion, out);
							}

						}

					} else if (ficheroHTMLpedido.startsWith("/accionDML101")) {
						if (params.isEmpty()) {
							sinParámetros(out);
						} else {

//                	Vamos a meterle el ID directamente

							String nom = params.get("NombreCompania");
							String des = params.get("NombreContacto");
							query = "INSERT INTO Proveedores ( NombreCompania, NombreContacto) VALUES ( '" + nom
									+ "', '" + des + "')";
							System.out.println(query);

							res = db.ejecutarUpdate(query);
							html = abrirHTML("Nuevo Proveedor Insertado")
									+ "<p> <a href='/menuSelect.html'> Volver al menú</a>" + cerrarHTML();
							out.println(html);
						}

					} else if (ficheroHTMLpedido.startsWith("/accionDML102")) {
						if (params.isEmpty()) {
							sinParámetros(out);
						} else {

							String nom = params.get("NombreProducto");
							String des = params.get("CantidadPorUnidad");
							query = "INSERT INTO Productos ( NombreProducto, CantidadPorUnidad) VALUES ( '" + nom
									+ "', '" + des + "')";
							System.out.println(query);

							res = db.ejecutarUpdate(query);
							html = abrirHTML("Nuevo Producto Insertado")
									+ "<p> <a href='/menuSelect.html'> Volver al menú</a>" + cerrarHTML();
							out.println(html);
						}

					
					} else {
						// Sirve páginas web estáticas desde un directorio "public"
						servirFicheroHTML(ficheroHTMLpedido, out);
					}

				} else {
					// Respuesta de error si la solicitud no es GET
					out.println("HTTP/1.1 405 Method Not Allowed");
				}
			}
			clientSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void servirFicheroHTML(String requestedFile, PrintWriter out) throws IOException {
		String filePath = "/home/a02/Documentos/SQL/Ejercicios_SQL_desde_entorno_programación/BBDD-05/raizWeb/"
				+ requestedFile;
		// El directorio "public" contiene los archivos estáticos
		File file = new File(filePath);

		if (file.exists() && file.isFile()) {
			BufferedReader fileIn = new BufferedReader(new FileReader(file));
			out.println("HTTP/1.1 200 OK");
			out.println("Content-Type: text/html"); // Asume que todos los archivos estáticos son HTML
			out.println();
			String line;
			while ((line = fileIn.readLine()) != null) {
				out.println(line);
			}
			fileIn.close();
		} else {
			// Error 404 si el archivo no se encuentra
			out.println("HTTP/1.1 404 Not Found");
		}
	}

	private static Map<String, String> obtenerParametros(String requestedFile) {
		Map<String, String> params = new HashMap<>();
		int paramsIndex = requestedFile.indexOf("?");
		if (paramsIndex > 0) {
			String paramsString = requestedFile.substring(paramsIndex + 1);
			String[] paramPairs = paramsString.split("&");
			for (String pair : paramPairs) {
				String[] keyValue = pair.split("=");
				if (keyValue.length == 2) {
					try {
						String key = URLDecoder.decode(keyValue[0], "UTF-8");
						String value = URLDecoder.decode(keyValue[1], "UTF-8");
						params.put(key, value);
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return params;
	}

	static void sinParámetros(PrintWriter out) {
		out.println(abrirHTML("No se han recibido parámetros") + cerrarHTML());
	}

	static String abrirHTML(String titulo) {
		String htm = "HTTP/1.1 200 OK \n Content-Type: text/html" + "\n \n"
				+ "\n <html lang='es'><head><meta charset='UTF-8'></head><body>" + "<h1> " + titulo + "</h1>";
		return htm;
	}

	static String cerrarHTML() {
		return "\n </body></html>";
	}

	static void lanzarDQL_responderHTM(int opcion, PrintWriter out) {
		List<String> cabeceras = null;
		String htmlTabla = "";
		String query = "";
		String titulo = "";
		switch (opcion) {
		case 1:
			query = "SELECT NombreProducto, CantidadPorUnidad,PrecioUnidad, UnidadesEnExistencia FROM Productos";
			cabeceras = List.of("Nombre", "UnidadesPorCaja", "Precio", "Stock");
			titulo = "Listado de productos";
			break;
		case 2:
			query = "SELECT DISTINCT Pais FROM clientes";
			cabeceras = List.of("Pais");
			titulo = "Listado de clientes";
		case 3:
			query = "SELECT NombreCompania, ciudad  FROM Clientes WHERE Pais='Espana'";
			cabeceras = List.of("Nombre de la Compañía", "Ciudad");
			titulo = "Listado de clientes de España";
			break;
		case 4:
			query = "SELECT Id_categoria, NombreCategoria, Descripcion FROM Categorias";
			cabeceras = List.of("Id", "Nombre", "Descripción");
			titulo = "Listado de Categorías";
			break;

		case 5:
			query = "SELECT NombreProducto, CantidadPorUnidad, PrecioUnidad, UnidadesEnExistencia " + "FROM Productos "
					+ "WHERE PrecioUnidad > 20";
			cabeceras = List.of("NombreProducto", "CantidadPorUnidad", "PrecioUnidad", "UnidadesEnExistencia");
			titulo = "Listado de productos con precio superior a 20€";
			break;

		case 6:
			query = "SELECT Nombre, TelDomicilio " + "FROM Empleados " + "WHERE Ciudad='Londres'";
			cabeceras = List.of("Nombre", "Telefono");
			titulo = "Telefono de los empleados que viven en Londres";
			break;

		case 7:
			query = "SELECT Id_Pedido, CiudadDestinatario, Cargo " + "FROM Pedidos "
					+ "WHERE Cargo=(SELECT MAX(Cargo) FROM Pedidos)";
			cabeceras = List.of("ID del Pedido", "Ciudad a la que se envía", "Cargo");
			titulo = "Pedido con mayor coste de cargo";
			break;

		case 8:
			query = "SELECT Id_Proveedor, NombreCompania, NombreContacto FROM Proveedores";
			cabeceras = List.of("Id del Proveedor", "Nombre de la Compañía", "Nombre de Contacto");
			titulo = "Listado de Proveedores";
			break;

		}

		htmlTabla = db.ejecutarQuery(query, cabeceras);
		out.println(abrirHTML(titulo) + htmlTabla + cerrarHTML());
	}

	static void responderHTM_Formulario(int opcion, PrintWriter out) {
		String html = "";
		switch (opcion) {
		case 101:
			html = abrirHTML("Agregar Nuevo Proveedor") + "<form action='accionDML101' method='GET'>\n"
					+ "<label for='id_categoria'>ID Proveedor:(Generado automáticamente)</label><br>\n"
					+ "<input type='number' id='Id_Proveedor' name='Id_Proveedor' value='automático' disabled><br><br>\n"
					+ "<label for='NombreCompania'>Nombre de la Compañía:</label><br>\n"
					+ "<input type='text' id='NombreCompania' name='NombreCompania' required><br><br>\n"
					+ "<label for='NombreContacto'>Nombre del contacto:</label><br>\n"
					+ "<input id='NombreContacto' name='NombreContacto' required></input><br><br>\n"
					+ "<input type='submit' value='Guardar Producto'>\n" + "</form>\n" + cerrarHTML();
			out.println(html);
			break;

		case 102:
			html = abrirHTML("Agregar nuevo producto") + "<form action='accionDML102' method='GET'>\n"
					+ "<label for='id_Producto'>ID Producto:(Generado automáticamente)</label><br>\n"
					+ "<input type='number' id='Id_Producto' name='Id_Producto' value='automático' disabled><br><br>\n"
					+ "<label for='NombreCProducto'>Nombre del producto:</label><br>\n"
					+ "<input type='text' id='NombreProducto' name='NombreProducto' required><br><br>\n"
					+ "<label for='CantidadPorUnidad'>Cantidad por Unidad:</label><br>\n"
					+ "<input id='CantidadPorUnidad' name='CantidadPorUnidad' required></input><br><br>\n"
					+ "<input type='submit' value='Guardar Proveedor'>\n" + "</form>\n" + cerrarHTML();
			out.println(html);

			break;
		default:
			System.out.println("Opción no válida.");
		}

//        htmlTabla= db.ejecutarQuery(query, cabeceras);        
		// out.println(abrirHTML(titulo)+htmlTabla+cerrarHTML());
	}

}