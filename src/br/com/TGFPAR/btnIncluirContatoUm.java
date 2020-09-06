package br.com.TGFPAR;

import java.math.BigDecimal;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btnIncluirContatoUm implements AcaoRotinaJava {

	public void doAction(ContextoAcao arg0) throws Exception {
		start(arg0);
	}
	
	private void start(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		BigDecimal codparceiro = (BigDecimal) linhas[0].getCampo("CODPARC");
		String parceiro = (String) linhas[0].getCampo("NOMEPARC");
		
		cadastraNaCtt(codparceiro,parceiro);
	}
	
	private void cadastraNaCtt(BigDecimal codparceiro, String parceiro) throws Exception {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("Contato");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODCONTATO", new BigDecimal(1));
			VO.setProperty("ATIVO", "S");
			VO.setProperty("NOMECONTATO", parceiro);
			VO.setProperty("CODPARC", codparceiro);
			
			dwfFacade.createEntity("Contato", (EntityVO) VO);
			
		} catch (Exception e) {
			System.out.println("-*-*Nao foi possivel cadastrar o contato um para o parceiro: "+codparceiro);
		}
		
	}
}
