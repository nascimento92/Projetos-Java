package xTestes;

public class btn_parametro {

	public static void main(String[] args) {
		String texto  = "'V','R'";
		String[] partes = texto.split(",");
		for (String parte : partes) {
            System.out.println(parte);
        }
 
	}
}
