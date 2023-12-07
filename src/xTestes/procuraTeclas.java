package xTestes;

import java.util.Collection;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class procuraTeclas implements AcaoRotinaJava{

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Object produto = arg0.getParam("CODPROD");
		Object patrimonio = arg0.getParam("CODBEM");
		
		String localizados = "";
		
		//exemplo pela filial
		JapeWrapper DAO = JapeFactory.dao("teclas");
		
		JapeWrapper DAO = JapeFactory.dao("view");
		Collection<DynamicVO> listaDeTeclasLocalizadas = DAO.find("this.CODBEM=? AND this.CODPROD=?", produto, patrimonio);
		
		for(DynamicVO tecla : listaDeTeclasLocalizadas) {
			localizados+=tecla.asBigDecimal("TECLA")+",";
		}
		
		arg0.setMensagemRetorno("Teclas localizadas com o produto: "+produto+"<br/>"+localizados);
	}

}
