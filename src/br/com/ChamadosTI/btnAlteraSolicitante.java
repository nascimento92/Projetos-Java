package br.com.ChamadosTI;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btnAlteraSolicitante implements AcaoRotinaJava {

	public void doAction(ContextoAcao arg0) throws Exception {
		
		start(arg0);
	}
	
	private void start(ContextoAcao arg0) throws Exception {
		
		String codusu = (String) arg0.getParam("CODUSU");
		BigDecimal usuario = new BigDecimal(codusu);
				
		  if(codusu!=null) { 
			  Registro[] linhas = arg0.getLinhas();
			  linhas[0].setCampo("CODUSU", usuario); 
			  
			  BigDecimal numos = (BigDecimal) linhas[0].getCampo("NUMOS");
			  
			  enviarEmail(numos,usuario);
			  
			  alterarExecutanteOS(numos,usuario);
			  }
		 
	}
	
private void enviarEmail(BigDecimal numos, BigDecimal usuario) throws Exception {
		
	try {
		
		String mensagem = new String();
		
		mensagem = "Prezado,<br/><br/> "
				+ "A sua solicitação para o departamento de TI foi registrada, OS gerada: <b>"+numos+"</b>."
				+ "<br/><br/>Qualquer questão enviar um e-mail para sistemas@grancoffee.com.br"
				+ "<br/><br/>Atencionamente,"
				+ "<br/>Departamento TI"
				+ "<br/>Gran Coffee Comércio, Locação e Serviços S.A."
				+ "<br/>"
				+ "<img src=http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-pq.png  alt=\"\"/>";
		
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("MSDFilaMensagem");
		DynamicVO VO = (DynamicVO) NPVO;
		
		VO.setProperty("CODFILA", getUltimoCodigoFila());
		VO.setProperty("DTENTRADA", new Timestamp(System.currentTimeMillis()));
		VO.setProperty("MENSAGEM", mensagem.toCharArray());
		VO.setProperty("TIPOENVIO", "E");
		VO.setProperty("ASSUNTO", new String("CHAMADO - "+numos));
		VO.setProperty("EMAIL", getEmailUsuario(usuario));
		VO.setProperty("CODUSU", usuario);
		VO.setProperty("STATUS", "Pendente");
		VO.setProperty("CODCON", new BigDecimal(0));		
		
		dwfFacade.createEntity("MSDFilaMensagem", (EntityVO) VO);
		
		
	} catch (Exception e) {
		System.out.println("*-*-Não foi possivel enviar o e-mail!"+e.getMessage());
	}
		
	}
	
	private BigDecimal getUltimoCodigoFila() throws Exception {
		int count = 0;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT MAX(CODFILA)+1 AS CODFILA FROM TMDFMG");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getInt("CODFILA");
		}
		
		BigDecimal ultimoCodigo = new BigDecimal(count);
		
		return ultimoCodigo;
	}
	
	private String getEmailUsuario(BigDecimal codusu) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Usuario");
		DynamicVO VO = DAO.findOne("CODUSU=?",new Object[] { codusu });
		
		String email = VO.asString("EMAIL");
		
		return email;

	}
	
	private void alterarExecutanteOS(BigDecimal numos, BigDecimal usuario) throws Exception {
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("OrdemServico","this.NUMOS=?", new Object[] { numos }));
		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
		PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
		EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
		DynamicVO VO = (DynamicVO) NVO;
		
		VO.setProperty("CODATEND", usuario);
		VO.setProperty("CODUSUSOLICITANTE", usuario);
		VO.setProperty("CODUSURESP", usuario);
		
		itemEntity.setValueObject((EntityVO) VO);
		}
	}

}
