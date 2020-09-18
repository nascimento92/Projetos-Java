package br.com.grancoffee.ChamadosTI;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;
import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class evento_comunicacao implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		throw new PersistenceException(
				"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/>"+
				"\n<font size=\"20\"><b>Comunicações não podem ser excluidas!!</b></font>\n<br/><br/>");	
		
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		
		if(validaUsuario(getUsuLogado())) {
			if(validacoes(VO)) {
				throw new PersistenceException(
						"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/>"+
						"\n<font size=\"20\"><b>Antes de enviar uma comunicação classifique o chamado e defina um atendente!</b></font>\n<br/><br/>");
			}
		}
		
		start(arg0);
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		throw new PersistenceException(
				"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/>"+
				"\n<font size=\"20\"><b>Comunicações não podem ser alteradas, enviar uma nova comunicação!</b></font>\n<br/><br/>");			
	}
	
	private void start(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal idChamado = VO.asBigDecimal("ID");
		BigDecimal usuario = getUsuLogado();
		String comunicacao=VO.asString("TEXTO");
		
		DynamicVO chamado = getChamado(idChamado);
		BigDecimal solicitante = chamado.asBigDecimal("CODUSU");
		BigDecimal numos = chamado.asBigDecimal("NUMOS");
		BigDecimal atendente = chamado.asBigDecimal("ATENDENTE");
		Timestamp dtFim = chamado.asTimestamp("DTFECHAMENTO");
		String nomeCompleto = getTSIUSU(usuario).asString("NOMEUSUCPLT");
		String status="";
		
		if(dtFim!=null) {
			throw new PersistenceException(
					"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/>"+
					"\n<font size=\"20\"><b>Chamado encerrado, não é possivel enviar novas comunicações!</b></font>\n<br/><br/>");			
		}
		
		if(usuario.equals(solicitante)) {
			status="1";
			String email = getTSIUSU(atendente).asString("EMAIL");
			enviarEmail(numos,comunicacao,email,nomeCompleto);
			alterStatusChamado(status,idChamado);
		}else {
			status="7";
			String email = getTSIUSU(solicitante).asString("EMAIL");
			enviarEmail(numos,comunicacao,email,nomeCompleto);
			alterStatusChamado(status,idChamado);
		}
		
		salvaDados(VO);
	}
	
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
	    codUsuLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
	    return codUsuLogado;    	
	}
	
	private void salvaDados(DynamicVO VO) {
		VO.setProperty("CODUSU", getUsuLogado());
		VO.setProperty("DTCOMUNICACAO", new Timestamp(System.currentTimeMillis()));
	}
	
	private DynamicVO getChamado(BigDecimal id) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_CHAMADOTI");
		DynamicVO VO = DAO.findOne("ID=?",new Object[] { id });
		return VO;
	}
	
	private DynamicVO getTSIUSU(BigDecimal usuario) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Usuario");
		DynamicVO VO = DAO.findOne("CODUSU=?",new Object[] { usuario });
		return VO;
	}
	
	private void enviarEmail(BigDecimal numos, String comunicacao, String email, String nomeCompleto) throws Exception {
		
		try {
			String mensagem = new String();
			
			mensagem = "Prezado,<br/><br/> "
					+ "Referente ao chamado de número <b>"+numos+"</b>. Existe uma nova comunicação: "
					+ "<br/><br/><b>Responsável pela comunicação:</b> "+nomeCompleto
					+ "<br/><br/><b>Comunicação:</b> <i>"+comunicacao+"</i>"
					+ "<br/><br/><b>Esta é uma mensagem automática, por gentileza não respondê-la</b>"
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
			VO.setProperty("EMAIL", email);
			VO.setProperty("CODUSU", new BigDecimal(0));
			VO.setProperty("STATUS", "Pendente");
			VO.setProperty("CODCON", new BigDecimal(0));	
			VO.setProperty("CODSMTP", new BigDecimal(1));
			VO.setProperty("MAXTENTENVIO", new BigDecimal(3));
			VO.setProperty("TENTENVIO", new BigDecimal(0));
			VO.setProperty("REENVIAR", "N");		
			
			dwfFacade.createEntity("MSDFilaMensagem", (EntityVO) VO);
		} catch (Exception e) {
			System.out.println("## [ChamadosTI.evento_comunicacao] ## - NAO FOI POSSIVEL ENVIAR A COMUNICACAO"+e.getMessage());
			e.printStackTrace();
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
	
	private void alterStatusChamado(String status, BigDecimal id) {
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_CHAMADOTI",
					"this.ID=?", new Object[] { id}));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("STATUS", status);
				BigDecimal numos = VO.asBigDecimal("NUMOS");
				itemEntity.setValueObject(NVO);
				alteraStatusOs(status,numos);
			}
	
		} catch (Exception e) {
			System.out.println("## [ChamadosTI.evento_comunicacao] NAO FOI POSSIVEL ALTERAR O STATUS DO CHAMADO!"+e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void alteraStatusOs(String status, BigDecimal numos) throws Exception {
				
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("OrdemServico",
					"this.NUMOS=?", new Object[] { numos }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("CODCOS", new BigDecimal(status));
				itemEntity.setValueObject(NVO);
				alteraCorSubOS(numos,status);
			}
			
		} catch (Exception e) {
			System.out.println("## [ChamadosTI.btn_statusOS] NAO FOI POSSIVEL ALTERAR O STATUS DA OS!"+e.getMessage());
			e.printStackTrace();
		}

	}
	
	private void alteraCorSubOS(BigDecimal numos, String status) {
		
		BigDecimal cor = null;
		
		switch (status) {
		
		case "1": cor=new BigDecimal(11909048); //pendente
			break;
		case "7": cor=new BigDecimal(16113568); //aguardando usuario
			break;

		default:
			break;
		}
		
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("ItemOrdemServico",
					"this.NUMOS=?", new Object[] { numos }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("CORSLA", cor);

				itemEntity.setValueObject(NVO);
			}

		} catch (Exception e) {
			System.out.println("## [ChamadosTI.btn_statusOS] NAO FOI POSSIVEL ALTERAR A COR DO SLA!"+e.getMessage());
			e.printStackTrace();
		}
	}
	
	private boolean validaUsuario(BigDecimal usuario) throws Exception {
		boolean valida=false;
		JapeWrapper DAO = JapeFactory.dao("Usuario");
		DynamicVO VO = DAO.findOne("CODUSU=?",new Object[] { usuario });
		String visualizaTodosOsChamados = VO.asString("AD_CHAMADOSTI");
		if("S".equals(visualizaTodosOsChamados)) {
			valida=true;
		}
		return valida;
	}
	
	private boolean validacoes(DynamicVO VO) throws Exception {
		boolean valida = false;
		BigDecimal idChamado = VO.asBigDecimal("ID");
		DynamicVO chamado = getChamado(idChamado);
		String tipo = chamado.asString("TIPO");
		BigDecimal atendimento = chamado.asBigDecimal("ATENDENTE");
		
		if(tipo==null) {
			valida=true;
		}else {valida=false;}
		
		if(atendimento==null) {
			valida=true;
		}
		
		return valida;
	}
}
