package xTestes;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class listas {
	public static void main(String[] args) {
		Object[] crLista1 = {10206000,10501001,10501002,10402154};
		List<Object> lista1 = Arrays.asList(crLista1);
		
		BigDecimal comparacao = new BigDecimal(102060002);
		
		if(lista1.contains(comparacao.intValue())) {
			System.out.println("existe");
		}else {
			System.out.println("não existe");
		}
	}
}
