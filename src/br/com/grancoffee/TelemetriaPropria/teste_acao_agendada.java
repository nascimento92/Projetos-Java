package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;
import com.sankhya.util.TimeUtils;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
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

public class teste_acao_agendada implements ScheduledAction {

	@Override
	public void onTime(ScheduledActionContext arg0) {
		JapeSession.SessionHandle hnd = null;

		try {

			hnd = JapeSession.open();
			hnd.execWithTX(new JapeSession.TXBlock() {

				public void doWithTx() throws Exception {

					getListaPendente();
					
				}

			});
			

		} catch (Exception e) {
			//salvarException("[onTime] não foi possível iniciar a sessão! "+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void getListaPendente() {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("GCSolicitacoesAbastecimento", "this.STATUS = ? ", new Object[] { "1" }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);
				
				BigDecimal id = DynamicVO.asBigDecimal("ID");
				BigDecimal idretorno = DynamicVO.asBigDecimal("IDABASTECIMENTO");
				BigDecimal numosx = DynamicVO.asBigDecimal("NUMOS");
				String patrimonio = DynamicVO.asString("CODBEM");
				Timestamp data = DynamicVO.asTimestamp("DTAGENDAMENTO");
				//String reabastecimento = DynamicVO.asString("REABASTECIMENTO");
				String apenasvisita = DynamicVO.asString("APENASVISITA");
				

				int compareTo = data.compareTo(TimeUtils.getNow()); //comparação das datas
				
				if(numosx==null) {
					if(compareTo<=0) { //gerar Agora
						if("S".equals(apenasvisita)) { //apenas uma visita
							// TODO :: Registra os dados na AD_TROCADEGRADE.
							// TODO :: Gera a OS.
							// TODO :: Registra o dado da OS Gerada.
							
							BigDecimal numos = gerarCabecalhoOS(patrimonio, "");
							if(numos!=null) {
								insereItem(numos,patrimonio);
								//geraItemOS(numos, patrimonio);
								salvaNumeroOS(numos, patrimonio, id, idretorno);
								validaAD_TROCADEGRADE(patrimonio,numos);
							}
						}
					}
				}

			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	private BigDecimal gerarCabecalhoOS(String patrimonio, String motivo) throws Exception{
		
		BigDecimal parceiro = getParceiro(patrimonio);
		BigDecimal contrato = getContrato(patrimonio);
		
		BigDecimal numos = null;
		
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("OrdemServico",new BigDecimal(593595));
			DynamicVO NotaProdVO = ModeloNPVO.buildClone();
			
			BigDecimal usuario = new BigDecimal(0);

			NotaProdVO.setProperty("DHCHAMADA", TimeUtils.getNow());
			NotaProdVO.setProperty("DTPREVISTA",addDias(TimeUtils.getNow(),new BigDecimal(7)));
			NotaProdVO.setProperty("NUMOS",null); 
			NotaProdVO.setProperty("SITUACAO","P");
			NotaProdVO.setProperty("CODUSUSOLICITANTE",usuario);
			NotaProdVO.setProperty("CODUSURESP",usuario);
			NotaProdVO.setProperty("DESCRICAO",motivo);
			NotaProdVO.setProperty("AD_MANPREVENTIVA", "N");
			NotaProdVO.setProperty("AD_CHAMADOTI", "N");
			NotaProdVO.setProperty("AD_TELPROPRIA", "S");
			NotaProdVO.setProperty("CODATEND", usuario);
			NotaProdVO.setProperty("TEMPOSLA", new BigDecimal(7000));
			NotaProdVO.setProperty("AD_TELASAC", "S");
			NotaProdVO.setProperty("CODCOS", new BigDecimal(1));
			NotaProdVO.setProperty("CODPARC", parceiro);
			NotaProdVO.setProperty("NUMCONTRATO", contrato);
			NotaProdVO.setProperty("CODCONTATO", new BigDecimal(1));
			NotaProdVO.setProperty("CODUSUFECH", null);
			NotaProdVO.setProperty("DTFECHAMENTO", null);
			NotaProdVO.setProperty("DTALTER", null);
			NotaProdVO.setProperty("CODBEM", patrimonio);
			NotaProdVO.setProperty("SERIE", patrimonio);
			  
			dwfFacade.createEntity(DynamicEntityNames.ORDEM_SERVICO,(EntityVO) NotaProdVO);
			numos = NotaProdVO.asBigDecimal("NUMOS");
			
		} catch (Exception e) {
			salvarException(
					"[gerarCabecalhoOS] Nao foi possivel Gerar o cabeçalho da OS! Patrimonio "+patrimonio
							+ e.getMessage() + "\n" + e.getCause());
		}
		
		return numos;	
	}
	
	private void insereItem(BigDecimal numos, String patrimonio) {
		
		try {
			
			BigDecimal motivo = new BigDecimal(100);
			BigDecimal atendenteRota = getAtendenteRota(patrimonio);
			DynamicVO ad_patrimonio = getADPATRIMONIO(patrimonio);
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("ItemOrdemServico");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("NUMOS", numos);
			VO.setProperty("NUMITEM", new BigDecimal(1));
			VO.setProperty("HRINICIAL", null);
			VO.setProperty("HRFINAL", null);
			VO.setProperty("DHPREVISTA", addDias(TimeUtils.getNow(),new BigDecimal(7)));
			VO.setProperty("INICEXEC", null);
			VO.setProperty("TERMEXEC", null);
			VO.setProperty("TEMPGASTO", null);
			VO.setProperty("CODSIT", new BigDecimal(1));
			VO.setProperty("CODOCOROS", motivo);
			VO.setProperty("SOLUCAO", " ");
			VO.setProperty("CODUSU", atendenteRota);
			VO.setProperty("CORSLA", null);
			VO.setProperty("CODUSUALTER", null);
			VO.setProperty("DTALTER", null);
			VO.setProperty("CODPROD", ad_patrimonio.asBigDecimal("CODPROD"));
			VO.setProperty("SERIE", patrimonio);
			VO.setProperty("AD_CODBEM1", patrimonio);
			VO.setProperty("AD_LONGITUDEINI", null);
			VO.setProperty("AD_LONGITUDEFIN", null);
			VO.setProperty("AD_DHLOCALIZACAOFIN", null);
			VO.setProperty("AD_LATITUDEFIN", null);
			VO.setProperty("AD_TELASAC", "S");
			VO.setProperty("CODSERV", new BigDecimal(200000));
			
			dwfFacade.createEntity("ItemOrdemServico", (EntityVO) VO);
			
		} catch (Exception e) {
			salvarException(
					"[insereItem] Nao foi possivel inserir o item! Patrimonio "+patrimonio
							+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void geraItemOS(BigDecimal numos, String patrimonio) throws Exception{
		

		BigDecimal atendenteRota = getAtendenteRota(patrimonio);
		BigDecimal motivo = new BigDecimal(100);
		DynamicVO ad_patrimonio = getADPATRIMONIO(patrimonio);
		BigDecimal servico = new BigDecimal(200000);
		
		try {
			cadastraServicoParaOhExecutante(ad_patrimonio.asBigDecimal("CODPROD"), atendenteRota, servico);
		} catch (Exception e) {
			// TODO: handle exception
		}
	
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("ItemOrdemServico",new Object[]{new BigDecimal(593595),new BigDecimal(1)});
			DynamicVO NotaProdVO = ModeloNPVO.buildClone();
			
			NotaProdVO.setProperty("NUMOS",numos);
			NotaProdVO.setProperty("NUMITEM",new BigDecimal(1));
			NotaProdVO.setProperty("HRINICIAL", null); 
			NotaProdVO.setProperty("HRFINAL", null);
			NotaProdVO.setProperty("DHPREVISTA", addDias(TimeUtils.getNow(),new BigDecimal(7)));
			NotaProdVO.setProperty("INICEXEC", null); 
			NotaProdVO.setProperty("TERMEXEC", null); 
			NotaProdVO.setProperty("TEMPGASTO", null);
			NotaProdVO.setProperty("CODSIT", new BigDecimal(1));
			NotaProdVO.setProperty("CODOCOROS", motivo);
			NotaProdVO.setProperty("SOLUCAO", " ");
			NotaProdVO.setProperty("CODUSU", atendenteRota);
			NotaProdVO.setProperty("CORSLA", null);
			NotaProdVO.setProperty("CODUSUALTER", null);
			NotaProdVO.setProperty("DTALTER", null);
			NotaProdVO.setProperty("CODPROD", ad_patrimonio.asBigDecimal("CODPROD"));
			NotaProdVO.setProperty("SERIE", patrimonio);
			NotaProdVO.setProperty("AD_CODBEM1", patrimonio);
			NotaProdVO.setProperty("AD_LONGITUDEINI", null);
			NotaProdVO.setProperty("AD_LONGITUDEFIN", null);
			NotaProdVO.setProperty("AD_DHLOCALIZACAOFIN", null);
			NotaProdVO.setProperty("AD_LATITUDEFIN", null);
			NotaProdVO.setProperty("AD_TELASAC", "S");
			
			dwfFacade.createEntity(DynamicEntityNames.ITEM_ORDEM_SERVICO,(EntityVO) NotaProdVO);


		} catch (Exception e) {
			salvarException(
					"[geraItemOS] Nao foi possivel Gerar a sub-os! Patrimonio "+patrimonio
							+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void salvaNumeroOS(BigDecimal numos, String patrimonio, BigDecimal idSolicitacao, BigDecimal idRetorno) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("GCSolicitacoesAbastecimento","this.CODBEM=? AND this.ID=? ", new Object[] { patrimonio,idSolicitacao }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			DynamicVO VO = (DynamicVO) NVO;

			VO.setProperty("NUMOS", numos);

			itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			salvarException("[salvaNumeroDaNota] Nao foi possivel salvar o numero da OS! patrimonio "+patrimonio+" abastecimento novo."+e.getMessage()+"\n"+e.getCause()); 
		}
		
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_RETABAST","this.CODBEM=? AND this.ID=? ", new Object[] { patrimonio,idRetorno }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			DynamicVO VO = (DynamicVO) NVO;

			VO.setProperty("NUMOS", numos);

			itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			salvarException("[salvaNumeroDaNota] Nao foi possivel salvar o numero da OS! patrimonio "+patrimonio+" abastecimento novo."+e.getMessage()+"\n"+e.getCause()); 
		}
	}
	
	private void validaAD_TROCADEGRADE(String patrimonio, BigDecimal numos) {
		
		boolean primeiraVisita = validaSeEhAhPrimeiraVisita(patrimonio);
		
		if(primeiraVisita) { //é a primeira visita
			//pega itens da gc_planograma
			
			try {

				EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

				Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
						new FinderWrapper("GCPlanograma", "this.CODBEM = ? ", new Object[] { patrimonio }));

				for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

					PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
					DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
							.wrapInterface(DynamicVO.class);

					String tecla = DynamicVO.asString("TECLA");
					BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
					BigDecimal capacidade = DynamicVO.asBigDecimal("CAPACIDADE");
					BigDecimal nivelpar = DynamicVO.asBigDecimal("NIVELPAR");
					BigDecimal vlrpar = DynamicVO.asBigDecimal("VLRPAR");
					BigDecimal vlrfun = DynamicVO.asBigDecimal("VLRFUN");
					BigDecimal valorFinal = vlrpar.add(vlrfun);
					BigDecimal qtdabast = new BigDecimal(0);
					BigDecimal nivelalerta = DynamicVO.asBigDecimal("NIVELALERTA");
			
					insertAD_TROCADEGRADE(patrimonio, numos, produto, tecla, valorFinal, capacidade, nivelpar, qtdabast, "IGUAL", "IGUAL", nivelalerta, vlrpar, vlrfun);

				}

			} catch (Exception e) {
				salvarException("[validaTeclasGC_PLANOGRAMA] Nao foi possivel verificar as teclas! patrimonio " + patrimonio
						+ e.getMessage() + "\n" + e.getCause());
			}
			
		}else { //não é a primeira visita
			//pega itens da ad_planogramaatual
			
			try {

				EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

				Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
						new FinderWrapper("AD_PLANOGRAMAATUAL", "this.CODBEM = ? ", new Object[] { patrimonio }));

				for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

					PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
					DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
							.wrapInterface(DynamicVO.class);

					String tecla = DynamicVO.asString("TECLA");
					BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
					BigDecimal capacidade = DynamicVO.asBigDecimal("CAPACIDADE");
					BigDecimal nivelpar = DynamicVO.asBigDecimal("NIVELPAR");
					BigDecimal vlrpar = DynamicVO.asBigDecimal("VLRPAR");
					BigDecimal vlrfun = DynamicVO.asBigDecimal("VLRFUN");
					BigDecimal valorFinal = vlrpar.add(vlrfun);
					BigDecimal qtdabast = new BigDecimal(0);
					BigDecimal nivelalerta = DynamicVO.asBigDecimal("NIVELALERTA");
			
					insertAD_TROCADEGRADE(patrimonio, numos, produto, tecla, valorFinal, capacidade, nivelpar, qtdabast, "IGUAL", "IGUAL", nivelalerta, vlrpar, vlrfun);

				}

			} catch (Exception e) {
				salvarException("[validaTeclasGC_PLANOGRAMA] Nao foi possivel verificar as teclas! patrimonio " + patrimonio
						+ e.getMessage() + "\n" + e.getCause());
			}
		}	
		
	}
	
	private void insertAD_TROCADEGRADE(String patrimonio, BigDecimal numos, BigDecimal produto, String tecla, BigDecimal valorFinal, BigDecimal capacidade, BigDecimal nivelpar, 
			BigDecimal qtdabast, String statuspar, String statusvalor, BigDecimal nivelalerta, BigDecimal vlrpar, BigDecimal vlrfun) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_TROCADEGRADE");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("NUMOS", numos);
			VO.setProperty("CODPROD", produto);
			VO.setProperty("TECLA", tecla);
			VO.setProperty("VALOR", valorFinal);
			VO.setProperty("CAPACIDADE", capacidade);
			VO.setProperty("NIVELPAR", nivelpar);
			VO.setProperty("QTDABAST", qtdabast);
			VO.setProperty("QTDCONTAGEM", new BigDecimal(0));
			VO.setProperty("MOLAVAZIA", "N");
			VO.setProperty("STATUS_PAR", statuspar);
			VO.setProperty("STATUS_VLR", statusvalor);
			VO.setProperty("NIVELALERTA", nivelalerta);
			VO.setProperty("VLRPAR", vlrpar);
			VO.setProperty("VLRFUN", vlrfun);
			VO.setProperty("QTDRET", new BigDecimal(0));
			
			dwfFacade.createEntity("AD_TROCADEGRADE", (EntityVO) VO);
			
		} catch (Exception e) {
			salvarException("[validaTeclasGC_PLANOGRAMA] Nao foi possivel inserir na AD_TROCADEGRADE " + patrimonio
					+ e.getMessage() + "\n" + e.getCause());	 
		}
	}
	
	private boolean validaSeEhAhPrimeiraVisita(String patrimonio) {
		boolean valida = true;
		
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT COUNT(*) FROM AD_PLANOGRAMAATUAL WHERE CODBEM='"+patrimonio+"'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("COUNT(*)");
				if (count >= 1) {
					valida = false;
				}
			}
			
		} catch (Exception e) {
			salvarException("[validaSeEhAhPrimeiraVisita] Nao foi possivel validar se é a primeira visita! patrimonio " + patrimonio
					+ e.getMessage() + "\n" + e.getCause());
		}
		
		return valida;

	}
	
	private BigDecimal getAtendenteRota(String patrimonio) {
		BigDecimal executante = null;
		
		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("select NVL(CODABAST,815) as CODABAST from ad_rotatel where id in (select id from ad_rotatelins where codbem='"+patrimonio+"') and nvl(ROTATELPROPRIA,'N')='S' and rownum=1");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				executante = contagem.getBigDecimal("CODABAST");
			}

		} catch (Exception e) {
			salvarException("[getAtendenteRota] Nao foi possibel o atendente da rota! patrimonio "+ patrimonio + e.getMessage() + "\n" + e.getCause());
		}
		
		return executante;
	}
	
	private void cadastraServicoParaOhExecutante(BigDecimal produto, BigDecimal atendente, BigDecimal servico) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("ServicoProdutoExecutante");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODSERV", servico);
			VO.setProperty("CODUSU", atendente);
			VO.setProperty("CODPROD", produto);
			
			dwfFacade.createEntity("ServicoProdutoExecutante", (EntityVO) VO);
		} catch (Exception e) {
			
		}
	}
	
	private DynamicVO getADPATRIMONIO(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("PATRIMONIO");
		DynamicVO VO = DAO.findOne("CODBEM=?", new Object[] { patrimonio });
		return VO;
	}
	
	private BigDecimal getParceiro(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("PATRIMONIO");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		BigDecimal contrato = VO.asBigDecimal("NUMCONTRATO");
		
		DAO = JapeFactory.dao("Contrato");
		DynamicVO VOS = DAO.findOne("NUMCONTRATO=?",new Object[] { contrato });
		BigDecimal parceiro = VOS.asBigDecimal("CODPARC");
		
		return parceiro;
	}
	
	private Timestamp addDias(Timestamp datainicial,BigDecimal prazo){
		GregorianCalendar gcm = new GregorianCalendar();
		Date data = new Date(datainicial.getTime());
		gcm.setTime(data);
		gcm.add(Calendar.DAY_OF_MONTH, prazo.intValue());
		data = gcm.getTime();
		Timestamp dataInicialMaisPrazo = new Timestamp(data.getTime());
		
		return dataInicialMaisPrazo;
	}
	
	private BigDecimal getContrato(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("PATRIMONIO");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		BigDecimal contrato = VO.asBigDecimal("NUMCONTRATO");
		return contrato;
	}
	
	private void salvarException(String mensagem) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("OBJETO", "btn_visita");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);
			
			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);
			
		} catch (Exception e) {
			//aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [salvarException] ## - Nao foi possivel salvar a Exception! "+e.getMessage());
		}
	}	
	
}	
	/*
	private BigDecimal geraOS(String patrimonio) throws Exception {
		
		BigDecimal numos = null;
		
		BigDecimal parceiro = getParceiro(patrimonio);
		BigDecimal contrato = getContrato(patrimonio);
		
		BigDecimal executante = getAtendenteRota(patrimonio);
		DynamicVO ad_patrimonio = getADPATRIMONIO(patrimonio);
		BigDecimal produto = ad_patrimonio.asBigDecimal("CODPROD");
		
		String jsessionid = RealizarLogin();
		
		String bodyOS = bodyOS(parceiro, contrato, executante, patrimonio, "", produto);
		String url = urlOS()+jsessionid;
		
		StringBuilder respostaOS = Post_JSON(url,bodyOS);
		
		String numosString = getNumos(respostaOS);
		
		if(numosString!=null) {
			numos = new BigDecimal(numosString);
		}

		logOut(jsessionid);
		
		return numos;
	}
	
	private  String bodyOS(BigDecimal parceiro, BigDecimal contrato, BigDecimal executante, String patrimonio, String motivo, BigDecimal produto) {
		String body = "{"+
			    "\"serviceName\":\"SacSP.newOS\","+
			    "\"requestBody\":{"+
			        "\"config\":{"+
			            "\"CODPARC\":\""+parceiro+"\","+
			            "\"NUMCONTRATO\":\""+contrato+"\","+
			            "\"CODSERV\":\"200000\","+
			            "\"EXECUTANTE\":\""+executante+"\","+
			            "\"PARCEIRONESPRESSO\":\"\","+
			            "\"CODBEM\":\""+patrimonio+"\","+
			            "\"PROBLEMA\":\"ABASTECER BEM: "+patrimonio+"\","+
			            "\"MOTIVO\":\""+motivo+"\","+
			            "\"EMAIL\":\"\","+
			            "\"BENSVINCULADOS\":[{\"TAREFA\":\"\",\"CODBEM\":\""+patrimonio+"\",\"CODPROD\":\""+produto+"\"}]}}}";
		return body;
	}
	
	private String urlOS() {
		return "http://sankhya.grancoffee.com.br:8180/grancoffee-labsx/service.sbr?application=Place&outputType=json&serviceName=SacSP.newOS&mgeSession=";
	}
	
	private void logOut(String jsession) {
		String url = "http://sankhya.grancoffee.com.br:8180/mge/service.sbr?serviceName=MobileLoginSP.logout&mgeSession="+jsession;
		Post_JSON(url,"");
	}
	
	private String RealizarLogin() {
		String url = "http://sankhya.grancoffee.com.br:8180/mge/service.sbr?serviceName=MobileLoginSP.login&outputType=json";
		String body = "{\"serviceName\": \"MobileLoginSP.login\",\"requestBody\": {"+
			        "\"NOMUSU\": {"+
			            "\"$\": \"GCDIGITAL\"},"+
			        "\"INTERNO\": {"+
			            "\"$\": \"123456789\"},"+
			        "\"KEEPCONNECTED\": {"+
			            "\"$\": \"S\"}}}";
		StringBuilder post_JSON = Post_JSON(url,body);
		String jSession = getJSession(post_JSON);
		return jSession;
	}
	
	private static String getJSession(StringBuilder post_JSON) {

		String jSession = null;
		
		Object obj2= JSONValue.parse(post_JSON.toString());
		JSONObject jsonObject2 = (JSONObject) obj2;
		JSONObject b = (JSONObject) jsonObject2.get("responseBody");
		
		if(b!=null) {
			JSONObject c = (JSONObject) b.get("jsessionid");
			String string = c.values().toString();
			jSession = string.replace("[", "").replace("]", "");
		}
		
		return jSession;
	}
	
	private String getNumos(StringBuilder post_JSON) {
		
		String numos = null;
		
		Object obj2= JSONValue.parse(post_JSON.toString());
		JSONObject jsonObject2 = (JSONObject) obj2;
		JSONObject b = (JSONObject) jsonObject2.get("responseBody");
		
		if(b!=null){
			JSONObject c = (JSONObject) b.get("row");
			JSONObject d = (JSONObject) c.get("NUMOS");
			String numeroOs = d.values().toString();
			numos = numeroOs.replace("[", "").replace("]", "");
		}
		
		return numos;
	}
	
	
	
	private BigDecimal getAtendenteRota(String patrimonio) {
		BigDecimal executante = null;
		
		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("select NVL(CODABAST,815) as CODABAST from ad_rotatel where id in (select id from ad_rotatelins where codbem='"+patrimonio+"') and nvl(ROTATELPROPRIA,'N')='S' and rownum=1");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				executante = contagem.getBigDecimal("CODABAST");
			}

		} catch (Exception e) {
			salvarException("[getAtendenteRota] Nao foi possibel o atendente da rota! patrimonio "+ patrimonio + e.getMessage() + "\n" + e.getCause());
		}
		
		return executante;
	}
	
	
	
	
	
	
	
	public static StringBuilder Post_JSON(String query_url,String request){
		StringBuilder response = new StringBuilder();
		
		try {
			
			URL url = new URL(query_url);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			
			conn.setConnectTimeout(5000);
			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("POST");
			
			OutputStream os = conn.getOutputStream();
			byte[] b = request.getBytes("UTF-8");
			os.write(b);
			os.flush();
			os.close();
			
			InputStream in = new BufferedInputStream(conn.getInputStream());
			byte[] res = new byte[2048];
			int i = 0;
			
			while ((i = in.read(res)) != -1) {
				response.append(new String(res, 0, i));
			}
			in.close();

		} catch (Exception e) {
			System.out.println("ERRRO !"+e.getMessage());
		}
		
		return response;
	}
	
	

}
*/

/*
 * 	System.out.println(
				"\n\n\n\n######################################################################################################\n\n\n\n"+
				"\nPATRIMONIO: "+patrimonio+
				"\nDATA AGENDAMENTO: "+ data+
				"\nREABASTECIMENTO: "+ reabastecimento +
				"\nAPENAS VISITA: " + apenasvisita +
				"\nGERACAO: " + comparacaodasdatas +
				"\n\n\n\n######################################################################################################\n\n\n\n");
				*/
