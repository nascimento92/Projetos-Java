package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import com.sankhya.util.TimeUtils;
import Helpers.WSPentaho;
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
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import br.com.sankhya.ws.ServiceContext;

public class btn_visita_novo implements AcaoRotinaJava{
	
	/**
	 * @author Gabriel
	 * 16/10/2021 vs 1.0 Reacriado botão de visitas, implementado para gerar a OS no aperto do botão.
	 * 21/10/2021 vs 1.1 Alteração do método carregaTeclasNosItensDeAbast, o preenchimento dos itens estava vindo da gc_planograma e não pode ser, ele tem que pegar da grade atual.
	 */
	
	int cont = 0;
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		start(arg0);
	}

	private void start(ContextoAcao arg0) throws Exception {
		Timestamp dtVisita = (Timestamp) arg0.getParam("DTVISITA");
		String motivo = (String) arg0.getParam("MOTIVO");

		Registro[] linhas = arg0.getLinhas();

		for (int i = 0; i < linhas.length; i++) {

			int visitaPendente = validaSeExisteVisitasPendentes(linhas[i].getCampo("CODBEM").toString());
			boolean maquinaNaRota = validaSeAhMaquinaEstaNaRota(linhas[i].getCampo("CODBEM").toString());

			if (visitaPendente > 0) {
				arg0.mostraErro("O Patrimônio <b>"+linhas[i].getCampo("CODBEM").toString()+"</b> já possui uma visita pendente! não é possível gerar outra!");
			} else {
				if(!maquinaNaRota) {
					arg0.mostraErro("Patrimônio <b>"+linhas[i].getCampo("CODBEM").toString()+"</b> fora da Rota, não pode ser gerado uma visita!");
				}else {
					BigDecimal idretorno = cadastrarNovaVisita(linhas[i].getCampo("CODBEM").toString());
					if(idretorno!=null) {
						carregaTeclasNosItensDeAbast(linhas[i].getCampo("CODBEM").toString(),idretorno);
						
						DynamicVO gc_solicitabast = agendarVisita(linhas[i].getCampo("CODBEM").toString(), dtVisita, motivo,idretorno);
						cont++;
						
						int compareTo = dtVisita.compareTo(TimeUtils.getNow());
						
						if(compareTo<=0) {
							BigDecimal numos = gerarCabecalhoOS(linhas[i].getCampo("CODBEM").toString(), motivo);
							if(numos!=null) {
								geraItemOS(numos, linhas[i].getCampo("CODBEM").toString());
								salvaNumeroOS(numos, linhas[i].getCampo("CODBEM").toString(), gc_solicitabast.asBigDecimal("ID"), idretorno);
								
								validaAD_TROCADEGRADE(linhas[i].getCampo("CODBEM").toString(),numos);
							}
						}
						
					}
				}
			}
		}
		
		if(cont>0) {
			arg0.setMensagemRetorno("Foram agendado(s) <b>" + cont + "</b> visita(s)!");
		}else {
			arg0.setMensagemRetorno("Não foram agendadas visitas!");
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
	
	private void geraItemOS(BigDecimal numos, String patrimonio) throws Exception{
		

		BigDecimal atendenteRota = getAtendenteRota(patrimonio);
		BigDecimal motivo = new BigDecimal(100);
		DynamicVO ad_patrimonio = getADPATRIMONIO(patrimonio);
		BigDecimal servico = new BigDecimal(200000);
		
		cadastraServicoParaOhExecutante(ad_patrimonio.asBigDecimal("CODPROD"), atendenteRota, servico);
			
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
		
		BigDecimal atendenteRota = getAtendenteRota(patrimonio);
		
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
			
			if(atendenteRota!=null) {
				VO.setProperty("RESPABAST", atendenteRota);
			}

			itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			salvarException("[salvaNumeroDaNota] Nao foi possivel salvar o numero da OS! patrimonio "+patrimonio+" abastecimento novo."+e.getMessage()+"\n"+e.getCause()); 
		}
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
	
	private BigDecimal gerarCabecalhoOS(String patrimonio, String motivo) throws Exception{
		
		BigDecimal parceiro = getParceiro(patrimonio);
		BigDecimal contrato = getContrato(patrimonio);
		
		BigDecimal numos = null;
		
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("OrdemServico",new BigDecimal(593595));
			DynamicVO NotaProdVO = ModeloNPVO.buildClone();
			
			BigDecimal usuario = getUsuLogado();

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
	
	private Timestamp addDias(Timestamp datainicial,BigDecimal prazo){
		GregorianCalendar gcm = new GregorianCalendar();
		Date data = new Date(datainicial.getTime());
		gcm.setTime(data);
		gcm.add(Calendar.DAY_OF_MONTH, prazo.intValue());
		data = gcm.getTime();
		Timestamp dataInicialMaisPrazo = new Timestamp(data.getTime());
		
		return dataInicialMaisPrazo;
	}

	private int validaSeExisteVisitasPendentes(String patrimonio) throws Exception {
		int quantidade = 0;

		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT COUNT(*) AS QTD FROM GC_SOLICITABAST WHERE CODBEM='" + patrimonio + "' AND STATUS='1' AND APENASVISITA='S'");
		contagem = nativeSql.executeQuery();
		while (contagem.next()) {
			quantidade = contagem.getInt("QTD");
		}

		return quantidade;
	}
	
	private boolean validaSeAhMaquinaEstaNaRota(String patrimonio) throws Exception {
		boolean valida = false;
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT CASE WHEN EXISTS(SELECT CODBEM FROM AD_ROTATELINS WHERE CODBEM='"+patrimonio+"') THEN 'S' ELSE 'N' END AS VALIDA FROM DUAL");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				String verifica = contagem.getString("VALIDA");
				if("S".equals(verifica)) {
					valida=true;
				}
			}
			
		} catch (Exception e) {
			salvarException("[validaSeAhMaquinaEstaNaRota] Não foi possivel verificar se a maquina "+patrimonio+" esta na rota. "+e.getMessage()+"\n"+e.getCause());
		}
		return valida;	
	}

	private DynamicVO agendarVisita(String patrimonio, Timestamp dtVisita, String motivo,BigDecimal idretorno) {
		DynamicVO gc_solicitabast = null;
		
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("GCSolicitacoesAbastecimento");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("DTSOLICIT", TimeUtils.getNow());
			VO.setProperty("DTAGENDAMENTO", dtVisita);
			VO.setProperty("CODUSU", getUsuLogado());
			VO.setProperty("STATUS", "1");
			VO.setProperty("ROTA", new BigDecimal(getRota(patrimonio)));
			VO.setProperty("MOTIVO", motivo.toCharArray());
			VO.setProperty("IDABASTECIMENTO", idretorno);
			VO.setProperty("APENASVISITA", "S");
			VO.setProperty("REABASTECIMENTO", "N");
			VO.setProperty("AD_NUMCONTRATO", getContrato(patrimonio));
			VO.setProperty("AD_CODPARC", getParceiro(patrimonio));

			dwfFacade.createEntity("GCSolicitacoesAbastecimento", (EntityVO) VO);
			
			gc_solicitabast = VO;

		} catch (Exception e) {
			salvarException("[agendarVisita] Não foi possivel agendar a visita! "+e.getMessage()+"\n"+e.getCause());
		}
		
		return gc_solicitabast;
	}

	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
		codUsuLogado = ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID();
		return codUsuLogado;
	}

	private int getRota(String patrimonio) {
		int count = 0;
		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT ID FROM AD_ROTATEL WHERE ID IN (SELECT ID FROM AD_ROTATELINS WHERE codbem='"
					+ patrimonio + "') AND ROWNUM=1");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				count = contagem.getInt("ID");
			}

		} catch (Exception e) {
			salvarException("[getRota] Não foi possivel obter a rota! "+e.getMessage()+"\n"+e.getCause());
		}

		return count;
	}

	private BigDecimal cadastrarNovaVisita(String patrimonio) {
		BigDecimal idAbastecimento = null;

		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_RETABAST");
			DynamicVO VO = (DynamicVO) NPVO;

			int rota = getRota(patrimonio);

			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("DTSOLICITACAO", TimeUtils.getNow());
			VO.setProperty("STATUS", "1");
			VO.setProperty("SOLICITANTE", getUsuLogado());
			VO.setProperty("APENASVISITA", "S");
			VO.setProperty("NUMCONTRATO", getContrato(patrimonio));
			VO.setProperty("CODPARC", getParceiro(patrimonio));

			if (rota != 0) {
				VO.setProperty("ROTA", new BigDecimal(rota));
			}

			dwfFacade.createEntity("AD_RETABAST", (EntityVO) VO);

			idAbastecimento = VO.asBigDecimal("ID");

		} catch (Exception e) {
			salvarException("[cadastrarNovaVisita] Não foi possivel registrar o retorno! "+e.getMessage()+"\n"+e.getCause());
		}

		return idAbastecimento;
	}

	private void carregaTeclasNosItensDeAbast(String patrimonio, BigDecimal idAbastecimento) throws Exception {

		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
				new FinderWrapper("AD_PLANOGRAMAATUAL", "this.CODBEM = ? ", new Object[] { patrimonio }));

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			String tecla = DynamicVO.asString("TECLA");
			BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
			BigDecimal capacidade = DynamicVO.asBigDecimal("CAPACIDADE");
			BigDecimal nivelpar = DynamicVO.asBigDecimal("NIVELPAR");
			BigDecimal vlrpar = DynamicVO.asBigDecimal("VLRPAR");
			BigDecimal vlrfun = DynamicVO.asBigDecimal("VLRFUN");
			BigDecimal valorFinal = vlrpar.add(vlrfun);
			
			try {

				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_ITENSRETABAST");
				DynamicVO VO = (DynamicVO) NPVO;

				VO.setProperty("ID", idAbastecimento);
				VO.setProperty("CODBEM", patrimonio);
				VO.setProperty("TECLA", tecla);
				VO.setProperty("CODPROD", produto);
				VO.setProperty("CAPACIDADE", capacidade);
				VO.setProperty("NIVELPAR", nivelpar);
				VO.setProperty("VALOR", valorFinal);

				dwfFacade.createEntity("AD_ITENSRETABAST", (EntityVO) VO);

			} catch (Exception e) {
				salvarException("[carregaTeclasNosItensDeAbast] Nao foi possivel salvar as teclas na tela Retornos! "+e.getMessage()+"\n"+e.getCause());
			}

		}
	}
	
	private BigDecimal getContrato(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("PATRIMONIO");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		BigDecimal contrato = VO.asBigDecimal("NUMCONTRATO");
		return contrato;
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
	
	private void chamaPentaho() {

		try {

			String site = (String) MGECoreParameter.getParameter("PENTAHOIP");;
			String Key = "Basic Z2FicmllbC5uYXNjaW1lbnRvOkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC_New/Transformation/Sankhya-Apenas_Visita/";
			String objName = "J-Loop_Apenas_visita";

			si.runJob(path, objName);

		} catch (Exception e) {
			salvarException("[chamaPentaho] nao foi possivel chamar o pentaho! "+e.getMessage()+"\n"+e.getCause());
		}
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
