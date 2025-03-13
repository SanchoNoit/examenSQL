import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

class DB {
    String url = null; // URL de conexión
    String usuario = null; //  usuario de MySQL
    String password = null; //  contraseña de MySQL

    Connection conexion=null;
    Statement sentencia=null;
    ResultSet resultado=null;
   
  

    DB(String url, String usuario, String password){
        this.url=url;
        this.usuario=usuario;
        this.password=password;
      
        sentencia=conectar();
  
      
    }
    Statement conectar(){
        Statement sentencia=null;
        try {
            //1.- Cargar el driver JDBC para MySQL que está en mysql-connector-j-9.1.0.jar
            Class.forName("com.mysql.cj.jdbc.Driver");

            //2.- Establecer la conexión
            // Conecta al servidor MySQL. La URL no incluye el nombre de una base de datos específica, 
            // ya que queremos listar todas las bases de datos.
            conexion = DriverManager.getConnection(url,usuario,password);

            //3.- Crea un objeto Statement para ejecutar consultas SQL
            sentencia = conexion.createStatement();
            return sentencia;
        } catch (ClassNotFoundException e) {
            System.err.println("Error: No se pudo cargar el controlador JDBC: " + e.getMessage());
            return null;
        }catch(SQLException e ){
            System.err.println("Otro error JDBC: " + e.getMessage());
            return null;
        }
    }


    
    String resultSetToHtmlTable(ResultSet rs) throws SQLException {
        StringBuilder htmlTable = new StringBuilder();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        // Inicio de la tabla HTML
        htmlTable.append("<table border='1'>\n");
        // Encabezados de la tabla
        htmlTable.append("<tr>");
        for (int i = 1; i <= columnCount; i++) {
            htmlTable.append("<th>")
                        .append(metaData.getColumnName(i))
                        .append("</th>");
        }
        htmlTable.append("</tr>\n");

        // Filas con datos
        while (rs.next()) {
            htmlTable.append("<tr>");
            for (int i = 1; i <= columnCount; i++) {
                htmlTable.append("<td>")
                            .append(rs.getObject(i) != null ? rs.getObject(i).toString() : "")
                            .append("</td>");
            }
            htmlTable.append("</tr>\n");
        }

        // Cierre de la tabla
        htmlTable.append("</table>");
        return htmlTable.toString();
    }
   
    String ejecutarQuery(String query, List<String> cabecera) {
        String html="";
        try{
           
            resultado = sentencia.executeQuery(query);
            html=resultSetToHtmlTable(resultado);
            //out.println(html);
            return html;
      
        } catch ( SQLException e) {
            System.err.println("Error de consulta a la Base de Datos: \n" + e.getMessage());
            return "";
        }

    }

    int ejecutarUpdate(String query) {
        String html="";
        int res=0;
        try{
            res = sentencia.executeUpdate(query);
            return res;
      
        } catch ( SQLException e) {
            System.err.println("Error de consulta a la Base de Datos: \n" + e.getMessage());
            return 0;
        }
    }

void cerrar(){
    try{
        // Cerrar la conexión y los recursos
        resultado.close();
        sentencia.close();
        conexion.close();
    }catch ( SQLException e) {
        System.err.println("Error: " + e.getMessage());
    }
}

}
