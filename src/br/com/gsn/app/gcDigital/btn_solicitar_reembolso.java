package br.com.gsn.app.gcDigital;

import java.math.BigDecimal;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import br.com.sankhya.ws.ServiceContext;

public class btn_solicitar_reembolso implements AcaoRotinaJava{

	int i = 0;
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		
		Double valor = (Double) arg0.getParam("VALOR");
		String motivo = (String) arg0.getParam("MOTIVO");
		
		for (Registro r : linhas) {
			start(r, arg0, new BigDecimal(valor), motivo);
		}
		
		if (i>0) {
			arg0.setMensagemRetorno("Foram solicitados <b>"+i+"</b> reembolsos!");
		}
		 
	}
	
	private void start(Registro linha, ContextoAcao arg0, BigDecimal valor, String motivo) throws Exception {
		String uid = (String) linha.getCampo("USERID");
		String nome = (String) linha.getCampo("NOME");
		String email = (String) linha.getCampo("EMAIL");
		
		validacoes(valor);
		
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_REEMBOLSOAPP");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("USERID", uid);
			VO.setProperty("VALOR", valor);
			VO.setProperty("MOTIVO", motivo);
			VO.setProperty("CODUSU", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("DTSOLICIT", TimeUtils.getNow());
			VO.setProperty("NOME", nome);
			VO.setProperty("EMAIL", email);
			
			dwfFacade.createEntity("AD_REEMBOLSOAPP", (EntityVO) VO);
			
			this.i++;
			
		} catch (Exception e) {
			salvarException("[start] não foi possível salvar a solicitação de reembolso ! uid: "+uid+"\n"+e.getMessage()+"\n"+e.getCause());
		}
		
	}
	
	private void validacoes(BigDecimal valor) throws Exception {
		BigDecimal vlr = (BigDecimal) MGECoreParameter.getParameter("VLRMAXREEMB");
		
		if(valor.doubleValue()>vlr.doubleValue()) {
			throw new Error("<br/><b>ATENÇÃO</b><br/><br/>Valor solicitado é maior que o permitido ! Valor máximo: <b>R$ "+vlr.doubleValue()+"</b><br/><br/>");
		}
	}
	
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "btn_solicitar_reembolso");
			VO.setProperty("PACOTE", "br.com.gsn.app.gcDigital");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", new BigDecimal(0));
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

		} catch (Exception e) {
			// aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}

}
