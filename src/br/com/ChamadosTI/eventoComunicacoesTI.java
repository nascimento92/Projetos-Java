package br.com.ChamadosTI;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class eventoComunicacoesTI implements EventoProgramavelJava {
	
	final String MenssagemComunicacao = "Responder as dúvidas do departamento de TI com o máximo de detalhes por gentileza.";
	
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		
		start(arg0);
		
	}
	
	private void start(PersistenceEvent arg0) throws Exception{
		DynamicVO newVO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		
		String newComunicacaoTI = newVO.asString("COMTI");
		String oldComunicacaoTI = oldVO.asString("COMTI");
		
		String newComunicacaoUser = newVO.asString("COMUSER");
		String oldComunicacaoUser = oldVO.asString("COMUSER");
		
		String status = newVO.asString("STATUS");
		
		if("PENDENTE".equals(status) || "AGUARDANDO USUARIO".equals(status) || "EM EXECUCAO".equals(status)|| "EM APROVACAO".equals(status)) {
			
			BigDecimal numos = newVO.asBigDecimal("NUMOS");
			BigDecimal usuario = null;
			
			try {
				usuario = getUsuLogado();
			} catch (Exception e) {
				usuario = new BigDecimal(0);
			}
			
			BigDecimal idChamado = newVO.asBigDecimal("ID");
			BigDecimal codusu = newVO.asBigDecimal("CODUSU");
			
			if(newComunicacaoTI!=oldComunicacaoTI) {

				if(validaUsuario(usuario)) {
					
					newVO.setProperty("COMUSER", null);
					newVO.setProperty("STATUSCOMUNICACAO", "1");
					salvaComunicacao(newComunicacaoTI,null,usuario,idChamado);
					enviaEmailSolicitante(codusu,numos,newComunicacaoTI);
					
				}else {
					throw new PersistenceException(
							"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
							"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>Você não possui autorização para enviar uma comunicação como TI !!</b></font>\n\n\n");

				}
			}else
			
			if(newComunicacaoUser!=oldComunicacaoUser) {
				
				if(newComunicacaoUser!=MenssagemComunicacao) {
					newVO.setProperty("STATUSCOMUNICACAO", "2");
					salvaComunicacao(newComunicacaoTI,newComunicacaoUser,usuario,idChamado);
					enviaEmailTI(usuario,numos,newComunicacaoUser,newComunicacaoTI);
					
					if(validaUsuario(usuario)) {
						throw new PersistenceException(
								"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
								"\n\n\n\n<font size=\"15\" color=\"#008B45\">Area reservada para comunicação do <b>Solicitante</b> preencher na area reservada para o setor de TI!!</font>\n\n\n");
					}
				}else {
					newVO.setProperty("COMUSER", " ");
				}
			}
			
		}	
	}
	
	private void salvaComunicacao(String comunicacaoTI, String comunicacaoUser, BigDecimal usuario, BigDecimal id) throws Exception {
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_HISTCOMUNICACAOTI");
		DynamicVO VO = (DynamicVO) NPVO;
		
		VO.setProperty("DTCOMUNICACAO", new Timestamp(System.currentTimeMillis()));
		if(comunicacaoUser!=null) {
			VO.setProperty("COMUSER", comunicacaoUser);
		}
		if(comunicacaoTI!=null) {
			VO.setProperty("COMTI", comunicacaoTI);
		}
		VO.setProperty("CODUSU", usuario);
		VO.setProperty("ID", id);
		
		dwfFacade.createEntity("AD_HISTCOMUNICACAOTI", (EntityVO) VO);

	}
	
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
	    codUsuLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
	    return codUsuLogado;    	
	}
	
	private void enviaEmailSolicitante(BigDecimal usuario, BigDecimal numos, String comunicacao) throws Exception {
			String mensagem = new String();
			
			mensagem = "Prezado,<br/><br/> "
					+ "O departamento de TI enviou uma comunicação referente a OS: <b>"+numos+"</b>."
					+ "<br/><br/> \""+comunicacao+"\""
					+ "<br/><br/> Por gentileza acesse a tela <b>Chamados TI</b> para responder a comunicação."
					+ "<br/><br/> <b>Este é um e-mail automático, não responder!!</b>"
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
			VO.setProperty("EMAIL", tsiusu(usuario).asString("EMAIL"));
			VO.setProperty("CODUSU", usuario);
			VO.setProperty("STATUS", "Pendente");
			VO.setProperty("CODCON", new BigDecimal(0));		
			
			dwfFacade.createEntity("MSDFilaMensagem", (EntityVO) VO);
	}
	
	private void enviaEmailTI(BigDecimal usuario, BigDecimal numos, String comunicacaoUser, String comunicacaoTI) throws Exception {
		String mensagem = new String();
		
		DynamicVO userVO = tsiusu(usuario);
		String nomeUsuario = userVO.asString("NOMEUSU");
		String nomeCompleto = userVO.asString("NOMEUSUCPLT");
		BigDecimal codGrupo = userVO.asBigDecimal("CODGRUPO");
		
		DynamicVO grupoVO = tsigru(codGrupo);
		String departamento = grupoVO.asString("NOMEGRUPO");
		
		mensagem = "Prezado,<br/><br/> "
				+ "O solicitante respondeu a comunicação da OS: <b>"+numos+"</b>."
				+ "<br/><br/> <b>Comunicação TI: </b> \""+comunicacaoTI+"\""
				+ "<br/><br/> <b>Solicitante: </b> \""+nomeUsuario+"\""
				+ "<br/> <b>Nome Completo: </b> \""+nomeCompleto+"\""
				+ "<br/> <b>Departamento: </b> \""+departamento+"\""
				+ "<br/><br/> <b>Comunicação Solicitante: </b> \""+comunicacaoUser+"\""
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
		VO.setProperty("EMAIL", "ti@grancoffee.com.br");
		VO.setProperty("CODUSU", usuario);
		VO.setProperty("STATUS", "Pendente");
		VO.setProperty("CODCON", new BigDecimal(0));		
		
		dwfFacade.createEntity("MSDFilaMensagem", (EntityVO) VO);
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
	
	private DynamicVO tsiusu(BigDecimal usuario) throws Exception{
		JapeWrapper DAO = JapeFactory.dao("Usuario");
		DynamicVO VO = DAO.findOne("CODUSU=?",new Object[] { usuario });
		
		return VO;

	}
	
	private DynamicVO tsigru(BigDecimal grupo) throws Exception{
		JapeWrapper DAO = JapeFactory.dao("GrupoUsuario");
		DynamicVO VO = DAO.findOne("CODGRUPO=?",new Object[] { grupo });
		
		return VO;

	}
	
	private boolean validaUsuario(BigDecimal usuario) throws Exception {
		boolean valida = false;
		JapeWrapper DAO = JapeFactory.dao("Usuario");
		DynamicVO VO = DAO.findOne("CODUSU=?",new Object[] { usuario });
		
		if(VO!=null) {
			String chamadosTI = VO.asString("AD_CHAMADOSTI");
			
			if("S".equals(chamadosTI)) {
				valida = true;
			}
		}
		
		return valida;
	}

}
