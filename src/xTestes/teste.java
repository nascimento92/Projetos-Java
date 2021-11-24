package xTestes;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class teste{

	public static void main(String[] args) {
		
		BigDecimal qtdMinima = new BigDecimal(1);
		BigDecimal falta = new BigDecimal(3);
		
		/*
		 * if(falta.divide(qtdMinima, 2, RoundingMode.HALF_EVEN).doubleValue()==1) {
		 * System.out.println("pode"); }else { System.out.println("nao pode"); }
		 */
		
		if(falta.doubleValue()%qtdMinima.doubleValue()==0) {
			System.out.println("numero inteiro");
		}else {
			System.out.println("numero quebrado");
		}
		
		/*
		 * System.out.println("Qtd minima: "+qtdMinima+ "\nfalta: "+falta);
		 */
		
	}	
}
