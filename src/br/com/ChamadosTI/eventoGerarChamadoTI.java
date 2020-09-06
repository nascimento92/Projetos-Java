package br.com.ChamadosTI;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
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
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class eventoGerarChamadoTI implements EventoProgramavelJava {
	
	/**
	 * Objeto realiza a abertura de um chamado automaticamente ao registrar na tela Chamados TI.
	 * 
	 * @author gabriel.nascimento
	 * @version 1.0
	 */
	
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		//validaAlteracaoAfter(arg0);	
	}

	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		start(arg0);	
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		validaAlteracao(arg0);	
	}
	
	//1.0 (before insert)
	private void start(PersistenceEvent arg0) {
		
		try {
			
			//pegando as informações
			DynamicVO VO = (DynamicVO) arg0.getVo();
			BigDecimal usuario = getUsuLogado(); //pega usuario logado
			
			DynamicVO tsiusu = tsiusu(usuario); //pega as info. do usuario logado
			String email = tsiusu.asString("EMAIL");
			String problema = VO.asString("DESCRICAO");
			BigDecimal tipo = VO.asBigDecimal("TIPO");

			//Geração da OS/SUB-OS
			BigDecimal numos = gerarCabecalhoOS(usuario,problema); 
			geraItemOS(numos,tipo);
			
			//setando as informações
			VO.setProperty("CODUSU", usuario);
			VO.setProperty("NUMOS", numos);
			VO.setProperty("STATUS", "PENDENTE");
			VO.setProperty("NEW_STATUS", "1");
			VO.setProperty("DTSOLICITACAO", new Timestamp(System.currentTimeMillis()));
			
			//pega o Anexo
			Object anexo = VO.getProperty("ANEXO");
			
			if(numos!=null) { //se a OS foi gerada
				
				if(anexo!=null) { //Se existir anexo
					salvarAnexo(anexo,numos);
				}
				
				if(email!=null) { //se usuário possuir e-mail
					enviarEmail(numos,email);
				}	
			}
			
			
		} catch (Exception e) {
			System.out.println("## CHAMADOS TI - GERACAO DA OS ## - NAO FOI POSSIVEL GERAR A OS"+e.getMessage());
			e.getStackTrace();
		}
		
	}
	
	//1.1
	private BigDecimal gerarCabecalhoOS(BigDecimal usuario, String problema){
		
		BigDecimal numos = BigDecimal.ZERO;
		
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("OrdemServico",new BigDecimal(414937));
			DynamicVO NotaProdVO = ModeloNPVO.buildClone();
			
			Timestamp dataAtual=new Timestamp(System.currentTimeMillis());

			NotaProdVO.setProperty("DHCHAMADA", dataAtual);
			NotaProdVO.setProperty("DTPREVISTA",addDias(dataAtual,new BigDecimal(7)));
			NotaProdVO.setProperty("NUMOS",null); 
			NotaProdVO.setProperty("SITUACAO","P");
			NotaProdVO.setProperty("CODUSUSOLICITANTE",usuario);
			NotaProdVO.setProperty("CODUSURESP",usuario);
			NotaProdVO.setProperty("DESCRICAO",problema);
			NotaProdVO.setProperty("AD_MANPREVENTIVA", "N");
			NotaProdVO.setProperty("AD_CHAMADOTI", "S");
			NotaProdVO.setProperty("CODATEND", usuario);
			NotaProdVO.setProperty("TEMPOSLA", new BigDecimal(7000));
			NotaProdVO.setProperty("AD_TELASAC", "S");
			NotaProdVO.setProperty("CODCOS", new BigDecimal(1));
			  
			dwfFacade.createEntity(DynamicEntityNames.ORDEM_SERVICO,(EntityVO) NotaProdVO);
			numos = NotaProdVO.asBigDecimal("NUMOS");
						
			return numos;

		} catch (Exception e) {
			System.out.println("## CHAMADOS TI - GERACAO DA OS ## - NAO FOI POSSIVEL GERAR A OS"+e.getMessage());
			e.printStackTrace();
		}
		
		return numos;
	}
	
	//1.2
	private void geraItemOS(BigDecimal numos, BigDecimal tipo){
		
		Timestamp dataAtual=new Timestamp(System.currentTimeMillis());
		
		BigDecimal tipoSubOS = null;
		
		if(tipo.intValue()>=1000000 && tipo.intValue()<2000000){
			tipoSubOS = new BigDecimal(55);
		}else if(tipo.intValue()>=2000000 && tipo.intValue()<3000000){
			tipoSubOS = new BigDecimal(56);
		}else if(tipo.intValue()>=3000000 && tipo.intValue()<4000000){
			tipoSubOS = new BigDecimal(57);
		}else if(tipo.intValue()>=4000000 && tipo.intValue()<5000000){
			tipoSubOS = new BigDecimal(58);
		}else if(tipo.intValue()>=5000000 && tipo.intValue()<6000000){
			tipoSubOS = new BigDecimal(59);
		}else if(tipo.intValue()>=6000000 && tipo.intValue()<7000000){
			tipoSubOS = new BigDecimal(60);
		}else if(tipo.intValue()>=7000000 && tipo.intValue()<8000000){
			tipoSubOS = new BigDecimal(61);
		}else if(tipo.intValue()>=8000000 && tipo.intValue()<9000000){
			tipoSubOS = new BigDecimal(62);
		}else if(tipo.intValue()>=9000000 && tipo.intValue()<10000000){
			tipoSubOS = new BigDecimal(63);
		}else if(tipo.intValue()>=10000000 && tipo.intValue()<11000000){
			tipoSubOS = new BigDecimal(64);
		}else if(tipo.intValue()>=11000000 && tipo.intValue()<12000000){
			tipoSubOS = new BigDecimal(65);
		}else if(tipo.intValue()>=12000000 && tipo.intValue()<13000000){
			tipoSubOS = new BigDecimal(72);
		}else if(tipo.intValue()>=13000000 && tipo.intValue()<14000000){
			tipoSubOS = new BigDecimal(73);
		}else {
			tipoSubOS = new BigDecimal(69);
		}
		
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("ItemOrdemServico",new Object[]{new BigDecimal(414937),new BigDecimal(1)});
			DynamicVO NotaProdVO = ModeloNPVO.buildClone();
			
			NotaProdVO.setProperty("NUMOS",numos);
			NotaProdVO.setProperty("NUMITEM",new BigDecimal(1));
			NotaProdVO.setProperty("HRINICIAL", null); 
			NotaProdVO.setProperty("HRFINAL", null);
			NotaProdVO.setProperty("DHPREVISTA", addDias(dataAtual,new BigDecimal(7)));
			NotaProdVO.setProperty("INICEXEC", null); 
			NotaProdVO.setProperty("TERMEXEC", null); 
			NotaProdVO.setProperty("SERIE", null);
			NotaProdVO.setProperty("CODSIT", new BigDecimal(1));
			NotaProdVO.setProperty("CODOCOROS", tipoSubOS);
			NotaProdVO.setProperty("SOLUCAO", " ");
			NotaProdVO.setProperty("CODUSU", new BigDecimal(2238));
			NotaProdVO.setProperty("CORSLA", null);
			
			dwfFacade.createEntity(DynamicEntityNames.ITEM_ORDEM_SERVICO,(EntityVO) NotaProdVO);


		} catch (Exception e) {
			System.out.println("## CHAMADOS TI - GERACAO DA OS ## - NAO FOI POSSIVEL GERAR A SUB-OS OS: "+numos+" "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	//1.3
	private void salvarAnexo(Object anexo, BigDecimal numos) throws Exception {
		
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("Anexo");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODATA", numos);
			VO.setProperty("TIPO", "W");
			VO.setProperty("DESCRICAO", "Anexo");
			VO.setProperty("ARQUIVO", "Anexo.gif");
			VO.setProperty("CODUSU", getUsuLogado());
			VO.setProperty("DTALTER", new Timestamp(System.currentTimeMillis()));
			VO.setProperty("TIPOCONTEUDO", "N");
			VO.setProperty("CONTEUDO", anexo);
			VO.setProperty("EDITA", "N");
			VO.setProperty("SEQUENCIA", new BigDecimal(0));
			VO.setProperty("SEQUENCIAPR", new BigDecimal(0));
			
			dwfFacade.createEntity("Anexo", (EntityVO) VO);
		} catch (Exception e) {
			System.out.println("## CHAMADOS TI - GERACAO DA OS ## - NAO FOI POSSIVEL SALVAR O ANEXO DA OS"+e.getMessage());
			e.printStackTrace();
		}
		

	}
	
	//1.4
	private void enviarEmail(BigDecimal numos, String email) throws Exception {
		
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
			VO.setProperty("EMAIL", email);
			VO.setProperty("CODUSU", getUsuLogado());
			VO.setProperty("STATUS", "Pendente");
			VO.setProperty("CODCON", new BigDecimal(0));		
			
			dwfFacade.createEntity("MSDFilaMensagem", (EntityVO) VO);
		} catch (Exception e) {
			System.out.println("## CHAMADOS TI - GERACAO DA OS ## - NAO FOI POSSIVEL ENVIAR E-MAIL"+e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	//1.4.1
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
	
	//METODO COMPLEMENTAR (SOMAR DIAS A UMA DATA)
	private Timestamp addDias(Timestamp datainicial,BigDecimal prazo){
		GregorianCalendar gcm = new GregorianCalendar();
		Date data = new Date(datainicial.getTime());
		gcm.setTime(data);
		gcm.add(Calendar.DAY_OF_MONTH, prazo.intValue());
		data = gcm.getTime();
		Timestamp dataInicialMaisPrazo = new Timestamp(data.getTime());
		
		return dataInicialMaisPrazo;
	}
	
	//METODO COMPLEMENTAR (PEGA USUARIO LOGADO)
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
	    codUsuLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
	    return codUsuLogado;    	
	}
	
	//METODO COMPLEMENTAR (PEGA AS INFO. DO USUARIO LOGADO)
	private DynamicVO tsiusu(BigDecimal usuario) throws Exception{
		JapeWrapper DAO = JapeFactory.dao("Usuario");
		DynamicVO VO = DAO.findOne("CODUSU=?",new Object[] { usuario });	
		return VO;
	}
	
	//BEFORE UPDATE
	
	//2.0 
	private void validaAlteracao(PersistenceEvent arg0) throws Exception {
		
		DynamicVO newVO = (DynamicVO) arg0.getVo();
		String newDescricao = newVO.asString("DESCRICAO");
		String statusOS = newVO.asString("STATUS");

		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		String oldDescricao = oldVO.asString("DESCRICAO");

		
		if(newDescricao!=oldDescricao) {
			
			if("EM EXECUCAO".equals(statusOS)) {//chamado em execucao
				  
				  throw new PersistenceException(
						"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"
								+ "\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>A OS está sendo executada não pode ser alterada, qualquer questão entrar em contato com o setor de TI</b></font>\n\n\n");
			}
			else if("CONCLUIDO".equals(statusOS)) { //concluido
				  
				  throw new PersistenceException(
						"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"
								+ "\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>A OS está concluida não pode ser alterada</b></font>\n\n\n");
			}
			else if("CANCELADO".equals(statusOS)) { //cancelado
				  
				  throw new PersistenceException(
						"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"
								+ "\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>A OS está cancelada não pode ser alterada</b></font>\n\n\n");
			}
			else { 
					
					BigDecimal numos = newVO.asBigDecimal("NUMOS");
					String problema = newVO.asString("DESCRICAO");	
					alteraProblemaOS(numos,problema);
			}
			
		}	 
	}
	
	//2.1
	private void alteraProblemaOS(BigDecimal numos,String problema) throws Exception {
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("OrdemServico","this.NUMOS=? ", new Object[] { numos }));
	        for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
	        PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			DynamicVO VO2 = (DynamicVO) NVO;
			
			VO2.setProperty("DESCRICAO", problema);
			
			itemEntity.setValueObject(NVO);
	        }
			
		} catch (Exception e) {
			System.out.println("## CHAMADOS TI - GERACAO DA OS ## - NAO FOI POSSIVEL ALTERAR O PROBLEMA DA OS"+e.getMessage());
			e.printStackTrace();
		}
	}
		
	//NAO UTILIZADOS
	/*
	 * //3.0 private void validaAlteracaoAfter(PersistenceEvent arg0) throws
	 * Exception { DynamicVO newVO = (DynamicVO) arg0.getVo(); String status =
	 * newVO.asString("STATUS");
	 * 
	 * VO=newVO;
	 * 
	 * if(status!="CONCLUIDO" || status!="CANCELADO") {
	 * 
	 * BigDecimal usuarioLogado = getUsuLogado(); String statusChamado =
	 * newVO.asString("NEW_STATUS"); BigDecimal ordemServico =
	 * newVO.asBigDecimal("NUMOS");
	 * 
	 * if(usuarioLogado==null) { usuarioLogado = new BigDecimal(0); }
	 * 
	 * if(validaUsuario(usuarioLogado)) {
	 * alterarStatusOS(ordemServico,statusChamado); } }
	 * 
	 * }
	 * 
	 * //3.1 private boolean validaUsuario(BigDecimal usuarioLogado) throws
	 * Exception { boolean valida = false;
	 * 
	 * DynamicVO tsiusu = tsiusu(usuarioLogado);
	 * 
	 * String chamadoTI = tsiusu.asString("AD_CHAMADOSTI");
	 * 
	 * if("S".equals(chamadoTI)) { valida = true; }
	 * 
	 * return valida; }
	 * 
	 * //3.1.1 private void alterarStatusOS(BigDecimal numos,String statusChamado) {
	 * try {
	 * 
	 * BigDecimal status = null;
	 * 
	 * if("1".equals(statusChamado)) { //pendente status = new BigDecimal(1);
	 * VO.setProperty("STATUS", "PENDENTE"); } else if("3".equals(statusChamado)) {
	 * //em execucao status = new BigDecimal(2); VO.setProperty("STATUS",
	 * "EM EXECUCAO"); } else if("4".equals(statusChamado)) { //em aprovacao status
	 * = new BigDecimal(3); VO.setProperty("STATUS", "EM APROVACAO"); } else
	 * if("5".equals(statusChamado)) { //aguardando usuario status = new
	 * BigDecimal(7); VO.setProperty("STATUS", "AGUARDANDO USUARIO"); } else {
	 * status = new BigDecimal(1); VO.setProperty("STATUS", "PENDENTE"); }
	 * 
	 * // altera Ordem Serviço EntityFacade dwfEntityFacade =
	 * EntityFacadeFactory.getDWFFacade(); Collection<?> parceiro =
	 * dwfEntityFacade.findByDynamicFinder(new
	 * FinderWrapper("OrdemServico","this.NUMOS=? ", new Object[] { numos })); for
	 * (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
	 * PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
	 * EntityVO NVO = (EntityVO) ((DynamicVO)
	 * itemEntity.getValueObject()).wrapInterface(DynamicVO.class); DynamicVO VO2 =
	 * (DynamicVO) NVO;
	 * 
	 * String situacao = VO2.asString("SITUACAO");
	 * 
	 * if(situacao!="F") { VO2.setProperty("CODCOS", status); }
	 * 
	 * itemEntity.setValueObject(NVO); }
	 * 
	 * 
	 * } catch (Exception e) { System.out.
	 * println("## CHAMADOS TI - GERACAO DA OS ## - NAO FOI POSSIVEL ALTERAR O PROBLEMA DA OS"
	 * +e.getMessage()); e.printStackTrace(); } }
	 */
	

}
