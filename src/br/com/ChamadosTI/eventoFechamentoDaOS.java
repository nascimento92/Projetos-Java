package br.com.ChamadosTI;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
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
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class eventoFechamentoDaOS implements EventoProgramavelJava {
	/**
	 * Objeto que observa o encerramento da OS para alterar as informações na tela chamados TI
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
	
	//1.0
	private void start(PersistenceEvent arg0) throws Exception {
		
		try {
			
			DynamicVO VO = (DynamicVO) arg0.getVo();

			String situacao = VO.asString("SITUACAO");
			BigDecimal numos = VO.asBigDecimal("NUMOS");
			String chamadoTI = VO.asString("AD_CHAMADOTI");
			BigDecimal statusOS = VO.asBigDecimal("CODCOS");
			BigDecimal usuarioResponsavel = VO.asBigDecimal("CODUSURESP");
			
			if(statusOS.intValue()==4 || statusOS.intValue()==5) { //se for concluido ou cancelado
				if("F".equals(situacao)){ //se a OS está fechada
					
					if("S".equals(chamadoTI)) { //se for um chamado TI
						salvaInformacoesChamado(numos,statusOS,"A");
						verificaSubOS(numos);
						enviarEmail(numos,usuarioResponsavel);
					}
				}
			}
			
			else if (statusOS.intValue() == 1 || statusOS.intValue() == 2 || statusOS.intValue() == 3
					|| statusOS.intValue() == 7) {// se for qualquer status menos concluido e cancelado
				
				if("F".equals(situacao)) {
					if ("S".equals(chamadoTI)) {
						String validador = "C1";
						VO.setProperty("CODCOS", new BigDecimal(4));
						salvaInformacoesChamado(numos, statusOS,validador);
						verificaSubOS(numos);
						enviarEmail(numos,usuarioResponsavel);
					}
				}else if ("S".equals(chamadoTI)) { // se for um chamado TI
					salvaInformacoesChamado(numos, statusOS,"A");
				}
			}
			
		} catch (Exception e) {
			System.out.println("## CHAMADOS TI - FECHAMENTO OS ## - NAO FOI POSSIVEL SALVAR AS INFORMAÇÕES NA TELA CHAMADOS TI (START)"+e.getMessage());
			e.getStackTrace();
		}

	}
	
	//1.1
	private void salvaInformacoesChamado(BigDecimal numOS,BigDecimal statusOS, String validador) throws Exception {
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_CHAMADOSTI","this.NUMOS=? ", new Object[] { numOS }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			DynamicVO VO = (DynamicVO) NVO;
			
			if("C1".equals(validador)) {
				VO.setProperty("STATUS", "CONCLUIDO");
				VO.setProperty("DTFECHAMENTO", new Timestamp(System.currentTimeMillis()));
				VO.setProperty("CANCELADA", "N");
			}else {
				
				if(statusOS.intValue()==1) { //Status Pendente
					//VO.setProperty("NEW_STATUS", "1");
					VO.setProperty("STATUS", "PENDENTE");
					VO.setProperty("DTFECHAMENTO", null);
					VO.setProperty("CANCELADA", "N");
				}
				else if(statusOS.intValue()==2) {//em execucao
					//VO.setProperty("NEW_STATUS", "3");
					VO.setProperty("STATUS", "EM EXECUCAO");
					VO.setProperty("CANCELADA", "N");
				}
				else if(statusOS.intValue()==3) {//em aprovacao
					//VO.setProperty("NEW_STATUS", "4");
					VO.setProperty("STATUS", "EM APROVACAO");
					VO.setProperty("CANCELADA", "N");
				}
				else if(statusOS.intValue()==4) {//concluido
					//VO.setProperty("NEW_STATUS", "2");
					VO.setProperty("STATUS", "CONCLUIDO");
					VO.setProperty("DTFECHAMENTO", new Timestamp(System.currentTimeMillis()));
					VO.setProperty("CANCELADA", "N");
				}
				else if(statusOS.intValue()==5) {//cancelado
					//VO.setProperty("NEW_STATUS", "6");
					VO.setProperty("STATUS", "CANCELADO");
					VO.setProperty("DTFECHAMENTO", new Timestamp(System.currentTimeMillis()));
					VO.setProperty("CANCELADA", "S");
				}
				else if(statusOS.intValue()==7) {//Aguardando Usuario
					//VO.setProperty("NEW_STATUS", "5");
					VO.setProperty("STATUS", "AGUARDANDO USUARIO");
					VO.setProperty("CANCELADA", "N");
				}	
			}

			itemEntity.setValueObject((EntityVO) VO);
			}
			
		} catch (Exception e) {
			System.out.println("## CHAMADOS TI - FECHAMENTO OS ## - NAO FOI POSSIVEL SALVAR AS INFORMAÇÕES NA TELA CHAMADOS TI "+e.getMessage());
			e.getStackTrace();
		}
		
		
	}
	
	//1.2
	private void verificaSubOS(BigDecimal numOS) throws Exception {
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("ItemOrdemServico","this.NUMOS = ? ", new Object[] { numOS }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO VO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			BigDecimal usuario = VO.asBigDecimal("CODUSU");
			Timestamp dataAbertura = VO.asTimestamp("DHENTRADA");
			String solucao = VO.asString("SOLUCAO");
			BigDecimal sequencia = VO.asBigDecimal("NUMITEM");
			
			salvaResolucao(numOS, usuario, dataAbertura,solucao,sequencia);
			}
			
		} catch (Exception e) {
			System.out.println("## CHAMADOS TI - FECHAMENTO OS ## - NAO FOI POSSIVEL VERIFICAR AS INFORMACOES DA SUB-OS"+e.getMessage());
			e.getStackTrace();
		}
	}
	
	//1.2.1
	private void salvaResolucao(BigDecimal numos, BigDecimal usuario, Timestamp dataAtual, String solucao, BigDecimal sequencia) throws Exception {
			try {
				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_HISTCHAMADOSTI");
				DynamicVO VO = (DynamicVO) NPVO;
				
				VO.setProperty("NUMOS", numos);
				VO.setProperty("CODUSUATEND", usuario);
				VO.setProperty("DTABERTURA", dataAtual);
				VO.setProperty("DTFECHAMENTO", new Timestamp(System.currentTimeMillis()));
				VO.setProperty("SOLUCAO", solucao);
				VO.setProperty("SEQUENCIA", sequencia);
				
				dwfFacade.createEntity("AD_HISTCHAMADOSTI", (EntityVO) VO);
				
			} catch (Exception e) {
				System.out.println("## CHAMADOS TI - FECHAMENTO OS ## - NAO FOI POSSIVEL SALVAR AS INFORMACOES DA RESOLUCAO "+e.getMessage());
				e.getStackTrace();
			}
		}
	
	//1.3
	private void enviarEmail(BigDecimal numos, BigDecimal usuario) throws Exception {
		
		try {
			
			String mensagem = new String();
			
			mensagem = "Prezado,<br/><br/> "
					+ "A sua solicitação para o departamento de TI foi encerrada, OS: <b>"+numos+"</b>."
					+ "<br/><br/>Todas as resoluções podem ser verificadas na tela <b>Chamados TI</b>"
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
			VO.setProperty("EMAIL", tsiusu(usuario).asString("EMAIL"));
			VO.setProperty("CODUSU", usuario);
			VO.setProperty("STATUS", "Pendente");
			VO.setProperty("CODCON", new BigDecimal(0));		
			
			dwfFacade.createEntity("MSDFilaMensagem", (EntityVO) VO);
			
		} catch (Exception e) {
			System.out.println("## CHAMADOS TI - FECHAMENTO OS ## - NAO FOI POSSIVEL ENVIAR E-MAIL"+e.getMessage());
			e.getStackTrace();
		}
		
	}
	
	//1.3.1
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
	
	//1.3.2 (necessário para descobrir o e-mail do usuário)
	private DynamicVO tsiusu(BigDecimal usuario) throws Exception{
		JapeWrapper DAO = JapeFactory.dao("Usuario");
		DynamicVO VO = DAO.findOne("CODUSU=?",new Object[] { usuario });
		
		return VO;

	}
}
