public class Entrada {
    
    public static void main(String[] args) {
        int puerto = 8081;
        String raizWeb="/home/a02/Documentos/SQL/Ejercicios_SQL_desde_entorno_programación/BBDD-05/raizWEB/";
        SimpleWebServer SServer=  new SimpleWebServer(puerto, raizWeb);
        System.out.println("constructor");
        SServer.arrancar();
    }
}