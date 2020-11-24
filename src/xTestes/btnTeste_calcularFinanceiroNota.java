package xTestes;

import java.math.BigDecimal;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;

public class btnTeste_calcularFinanceiroNota implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Integer x = (Integer) arg0.getParam("NUNOTA");	
		BigDecimal nunota = new BigDecimal(x);
		totalizaImpostos(nunota);
	}
	
	public void totalizaImpostos(BigDecimal nunota) throws Exception{
        ImpostosHelpper impostos = new ImpostosHelpper();
        impostos.carregarNota(nunota);
       // impostos.forcaRecalculoBaseISS(true);
        impostos.setForcarRecalculo(true);
//        impostos.setForcarRecalculoIcmsZero(true);
//        impostos.setForcarRecalculoIpiZero(true); 
        impostos.calcularImpostos(nunota);
        impostos.calcularTotalItens(nunota, true);
        impostos.calculaICMS(true);
       // impostos.calcularPIS();         
       // impostos.calcularCSSL();
       // impostos.calcularCOFINS();
        impostos.totalizarNota(nunota);
        impostos.salvarNota();
        
	}

}
