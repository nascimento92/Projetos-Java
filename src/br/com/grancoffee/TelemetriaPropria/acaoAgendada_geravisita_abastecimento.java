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
import br.com.sankhya.modelcore.comercial.ComercialUtils;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.SPBeanUtils;
import br.com.sankhya.ws.ServiceContext;

public class acaoAgendada_geravisita_abastecimento implements ScheduledAction {

	/**
	 * 23/10/2021 vs 1.1 Inserido método insereItemEmRuptura para salvar os itens que deveriam ser abastecidos porém não tinha em estoque na filial
	 * 24/11/2021 vs 1.2 Ajustado a geração dos pedidos considerando a quantidade mínima.
	 */
	
	@Override
	public void onTime(ScheduledActionContext arg0) {
		
		ServiceContext sctx = new ServiceContext(null); 		
		sctx.setAutentication(AuthenticationInfo.getCurrent()); 
		sctx.makeCurrent();

		try {
			SPBeanUtils.setupContext(sctx);
		} catch (Exception e) {
			e.printStackTrace();
			salvarException("[onTime] não foi possível setar o usuário! "+e.getMessage()+"\n"+e.getCause());
		} 
				
		JapeSession.SessionHandle hnd = null;

		try {

			hnd = JapeSession.open();
			hnd.execWithTX(new JapeSession.TXBlock() {

				public void doWithTx() throws Exception {

					getListaPendente();
				}

			});
			

		} catch (Exception e) {
			salvarException("[onTime] não foi possível iniciar a sessão! "+e.getMessage()+"\n"+e.getCause());
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
				String reabastecimento = DynamicVO.asString("REABASTECIMENTO");
				String status = DynamicVO.asString("STATUS");
				BigDecimal nunota = DynamicVO.asBigDecimal("NUNOTA");
				BigDecimal substituto = DynamicVO.asBigDecimal("AD_USUSUB");
				
				int compareTo = data.compareTo(TimeUtils.getNow()); //comparação das datas
				
				if(numosx==null) {
					if(compareTo<=0) { //gerar Agora
						if("1".equals(status)) {
							if("S".equals(reabastecimento)) {
								//gerarPedidoENota(patrimonio, DynamicVO, idretorno, id);
								gerarPedidoENota(patrimonio, DynamicVO,idretorno,id, nunota, substituto);
							}
						}
					}
				}

			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	
	private void gerarPedidoENota(String patrimonio, DynamicVO gc_solicitabast, BigDecimal idRetorno, BigDecimal idsolicitacao, BigDecimal nto, BigDecimal sub) throws Exception {
		
		BigDecimal nunota = null;
		
		if(nto!=null) {
			nunota = nto;
		}else {
			nunota = geraCabecalho(patrimonio, gc_solicitabast);
		}
			
		if(nunota!=null) {
			
			if(nto==null) {
				identificaItens(nunota,patrimonio,gc_solicitabast);
				salvaNumeroDaNota(nunota,patrimonio, idsolicitacao, idRetorno);
				totalizaImpostos(nunota);
			}

			BigDecimal numos = gerarCabecalhoOS(patrimonio);
			
			if(numos!=null) {
				geraItemOS(numos, patrimonio, gc_solicitabast, sub);
				salvaNumeroOS(numos, patrimonio, idsolicitacao, idRetorno, gc_solicitabast, sub);
				
				verificaPlanogramaPendente(patrimonio, numos, nunota, idsolicitacao, idRetorno);
				
				validaAD_TROCADEGRADE(patrimonio, numos, nunota);
				validaItensDaTrocaDeGrade(patrimonio, numos);
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
			VO.setProperty("QTDRET", new BigDecimal(0));
			VO.setProperty("MOLAVAZIA", "N");
			VO.setProperty("STATUS_PAR", statuspar);
			VO.setProperty("STATUS_VLR", statusvalor);
			VO.setProperty("NIVELALERTA", nivelalerta);
			VO.setProperty("VLRPAR", vlrpar);
			VO.setProperty("VLRFUN", vlrfun);
			
			dwfFacade.createEntity("AD_TROCADEGRADE", (EntityVO) VO);
			
		} catch (Exception e) {
			salvarException("[validaTeclasGC_PLANOGRAMA] Nao foi possivel inserir na AD_TROCADEGRADE " + patrimonio
					+ e.getMessage() + "\n" + e.getCause());	 
		}
	}
		
	private void validaItensDaTrocaDeGrade(String patrimonio, BigDecimal numos) {
		
		//TODO :: verificar itens para retirar
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_PLANOGRAMAATUAL","this.CODBEM = ? ", new Object[] { patrimonio }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			String tecla = DynamicVO.asString("TECLA");
			BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
			BigDecimal vlrpar = DynamicVO.asBigDecimal("VLRPAR");
			BigDecimal vlrfun = DynamicVO.asBigDecimal("VLRFUN");
			BigDecimal valorFinal = vlrpar.add(vlrfun);	
			BigDecimal capacidade = DynamicVO.asBigDecimal("CAPACIDADE");
			BigDecimal nivelpar = DynamicVO.asBigDecimal("NIVELPAR");
			BigDecimal nivelalerta = DynamicVO.asBigDecimal("NIVELALERTA");
			
			boolean existeNoPlanogramaPendente = validaSeExisteNaPlanogramaPendenteOuAtual(patrimonio,produto,tecla,numos, "AD_PLANOGRAMAPENDENTE");
			
			if(!existeNoPlanogramaPendente) {
				//TODO :: inserir para retirar
				insertAD_TROCADEGRADE(patrimonio, numos, produto, tecla, valorFinal, capacidade, nivelpar, new BigDecimal(0), "RETIRAR", "RETIRAR", nivelalerta, vlrpar, vlrfun);
			}

			}
		} catch (Exception e) {
			salvarException("[validaItensDaTrocaDeGrade] Nao foi possivel verificar os produtos a serem retirados " + patrimonio
					+ e.getMessage() + "\n" + e.getCause());
		}
		
		//TODO :: verificar itens novos e já existentes
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_PLANOGRAMAPENDENTE","this.NUMOS = ? ", new Object[] { numos }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			String tecla = DynamicVO.asString("TECLA");
			BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
			BigDecimal vlrpar = DynamicVO.asBigDecimal("VLRPAR");
			BigDecimal vlrfun = DynamicVO.asBigDecimal("VLRFUN");
			BigDecimal valorFinal = vlrpar.add(vlrfun);	
			BigDecimal nivelpar = DynamicVO.asBigDecimal("NIVELPAR");
			
			boolean existeNaPlanogramaAtual = validaSeExisteNaPlanogramaPendenteOuAtual(patrimonio,produto,tecla,null,"AD_PLANOGRAMAATUAL");
			
			if(!existeNaPlanogramaAtual) { // produto novo
				atualizaStatusPlanogramaPendente(numos,produto,tecla,patrimonio,"NOVO", "NOVO");
			}else { //produto já existe
				DynamicVO planogramaAtual = getPlanogramaAtual(patrimonio, produto, tecla);
				BigDecimal parAtual = planogramaAtual.asBigDecimal("NIVELPAR");
				BigDecimal vlrParAtual = planogramaAtual.asBigDecimal("VLRPAR");
				BigDecimal vlrFunAtual = planogramaAtual.asBigDecimal("VLRFUN");
				BigDecimal valorFinalAtual = vlrParAtual.add(vlrFunAtual);
				
				String statusPar = "";
				String statusVlr = "";
				
				if(nivelpar.intValue()>parAtual.intValue()) {
					statusPar = "AUMENTO PAR";
				}else if (nivelpar.intValue()<parAtual.intValue()) {
					statusPar = "REDUCAO PAR";
				}else {
					statusPar = "IGUAL";
				}
				
				if(valorFinal.doubleValue()>valorFinalAtual.doubleValue()) {
					statusVlr = "AUMENTO VLR";
				}else if (valorFinal.doubleValue()<valorFinalAtual.doubleValue()) {
					statusVlr = "REDUCAO VLR";
				}else {
					statusVlr = "IGUAL";
				}
				
				atualizaStatusPlanogramaPendente(numos,produto,tecla,patrimonio,statusPar, statusVlr);
				
			}

			}
		} catch (Exception e) {
			salvarException("[validaItensDaTrocaDeGrade] Nao foi possivel verificar os produtos novos e existes " + patrimonio
					+ e.getMessage() + "\n" + e.getCause());
		}

	}
	
	private DynamicVO getPlanogramaAtual(String patrimonio, BigDecimal produto, String tecla) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_PLANOGRAMAATUAL");
		DynamicVO VO = DAO.findOne("CODBEM=? AND CODPROD=? AND TECLA=?", new Object[] { patrimonio,produto,tecla});
		return VO;
	}
	
	private void atualizaStatusPlanogramaPendente(BigDecimal numos, BigDecimal produto, String tecla, String patrimonio, String statuspar, String statusvalor) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_TROCADEGRADE",
					"this.NUMOS=? AND this.CODPROD=? AND this.TECLA=? AND this.CODBEM=? ", new Object[] { numos, produto,tecla,patrimonio }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("STATUS_PAR", statuspar);
				VO.setProperty("STATUS_VLR", statusvalor);

				itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			salvarException("[atualizaStatusPlanogramaPendente] Nao foi possivel atualizar o status na AD_TROCADEGRADE " + patrimonio + " produto "+produto
					+ e.getMessage() + "\n" + e.getCause());	 
		}
	}
	
	private boolean validaSeExisteNaPlanogramaPendenteOuAtual(String patrimonio, BigDecimal produto, String tecla, BigDecimal numos, String tabela) {
		boolean valida = false;
		
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			
			if(numos!=null) {
				nativeSql.appendSql("SELECT COUNT(*) AS QTD FROM "+tabela+" WHERE CODBEM='"+patrimonio+"' AND CODPROD='"+produto+"' AND TECLA='"+tecla+"' AND NUMOS="+numos);
			}else {
				nativeSql.appendSql("SELECT COUNT(*) AS QTD FROM "+tabela+" WHERE CODBEM='"+patrimonio+"' AND CODPROD='"+produto+"' AND TECLA='"+tecla+"'");
			}
			
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("QTD");
				if (count >= 1) {
					valida = true;
				}
			}

			
		} catch (Exception e) {
			salvarException("[validaSeExisteNoPlanogramaAtual] Nao foi possivel validar se existe a tecla na ad_planogramapendente! patrimonio " + patrimonio+ " tecla "+tecla
					+ e.getMessage() + "\n" + e.getCause());
		}
		
		return valida;
	}
	
	private void validaAD_TROCADEGRADE(String patrimonio, BigDecimal numos, BigDecimal nunota) {
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
				BigDecimal nivelalerta = DynamicVO.asBigDecimal("NIVELALERTA");

				BigDecimal qtdabast = null;
				if (nunota != null) {
					qtdabast = validaQuantidadeParaAbastecer(nunota, tecla, produto);
				} else {
					qtdabast = new BigDecimal(0);
				}

				insertAD_TROCADEGRADE(patrimonio, numos, produto, tecla, valorFinal, capacidade, nivelpar, qtdabast, "", "", nivelalerta, vlrpar, vlrfun);

			}

		} catch (Exception e) {
			salvarException("[validaTeclasGC_PLANOGRAMA] Nao foi possivel verificar as teclas! patrimonio " + patrimonio
					+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private BigDecimal validaQuantidadeParaAbastecer(BigDecimal nunota, String tecla, BigDecimal produto) {
		BigDecimal qtdabast = BigDecimal.ZERO;
		
		try {
			JapeWrapper DAO = JapeFactory.dao("ItemNota");
			DynamicVO VO = DAO.findOne("NUNOTA=? and CODPROD=? and AD_TECLA=?",new Object[] { nunota, produto, tecla });
			
			if(VO!=null) {
				qtdabast = VO.asBigDecimal("QTDNEG");
			}

		} catch (Exception e) {
			salvarException(
					"[validaQuantidadeParaAbastecer] Nao foi possivel validar a quantidade para abastecer! Nunota "+nunota
							+ e.getMessage() + "\n" + e.getCause());
		}
		
		return qtdabast;
	}
	
	private void verificaPlanogramaPendente(String patrimonio, BigDecimal numos, BigDecimal nunota, BigDecimal idSolicitacao, BigDecimal idRetorno) {
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("GCPlanograma","this.CODBEM = ?", new Object[] { patrimonio }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			
			String tecla = (String) DynamicVO.getProperty("TECLA");
			BigDecimal produto = (BigDecimal) DynamicVO.getProperty("CODPROD");
			BigDecimal capacidade = (BigDecimal) DynamicVO.getProperty("CAPACIDADE");
			BigDecimal nivelpar = (BigDecimal) DynamicVO.getProperty("NIVELPAR");
			BigDecimal nivelalerta = (BigDecimal) DynamicVO.getProperty("NIVELALERTA");
			BigDecimal estoque = validaEstoqueDoItem(DynamicVO.asBigDecimal("ESTOQUE"));
			BigDecimal vlrpar = (BigDecimal) DynamicVO.getProperty("VLRPAR");
			BigDecimal vlrfun = (BigDecimal) DynamicVO.getProperty("VLRFUN");
			
			salvaPlanogramaPendente(patrimonio,tecla,produto,numos,nunota,idSolicitacao,idRetorno,capacidade,nivelpar,nivelalerta,estoque,vlrpar,vlrfun);
			
			}
		} catch (Exception e) {
			salvarException(
					"[verificaPlanogramaPendente] Nao foi possivel verifica os itens para o planograma pendente! patrimonio "+patrimonio
							+ e.getMessage() + "\n" + e.getCause());
		}
		
	}
	
	private void salvaPlanogramaPendente(String patrimonio, String tecla, BigDecimal produto, BigDecimal numos, BigDecimal nunota, BigDecimal idSolicitacao, BigDecimal idRetorno,
			BigDecimal capacidade, BigDecimal nivelpar, BigDecimal nivelalerta, BigDecimal estoque, BigDecimal vlrpar, BigDecimal vlrfun) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_PLANOGRAMAPENDENTE");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("TECLA", tecla);
			VO.setProperty("CODPROD", produto);
			VO.setProperty("NUMOS", numos);
			VO.setProperty("NUNOTA", nunota);
			VO.setProperty("IDSOLICIT", idSolicitacao);
			VO.setProperty("IDRETORNO", idRetorno);
			VO.setProperty("CAPACIDADE", capacidade);
			VO.setProperty("NIVELPAR", nivelpar);
			VO.setProperty("NIVELALERTA", nivelalerta);
			VO.setProperty("ESTOQUE", estoque);
			VO.setProperty("VLRPAR", vlrpar);
			VO.setProperty("VLRFUN", vlrfun);
			
			dwfFacade.createEntity("AD_PLANOGRAMAPENDENTE", (EntityVO) VO);
			
		} catch (Exception e) {
			salvarException(
					"[salvaPlanogramaPendente] Nao foi possivel salvar o planograma pendente! patrimonio "+patrimonio
							+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private BigDecimal validaEstoqueDoItem(BigDecimal est) {
		BigDecimal estoque = null;
		if(est!=null) {
			estoque = est;
		}else {
			estoque = new BigDecimal(0);
		}
		return estoque;
	}
	
	private void salvaNumeroOS(BigDecimal numos, String patrimonio, BigDecimal idSolicitacao, BigDecimal idRetorno, DynamicVO gc_solicitabast, BigDecimal sub) {
		
		BigDecimal atendenteRota = null;
		
		if(sub!=null) {
			atendenteRota = sub;
		}else {
			atendenteRota = getAtendenteRota(patrimonio, gc_solicitabast);
		}
		
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
	
	private BigDecimal getAtendenteRota(String patrimonio, DynamicVO gc_solicitabast) {
		BigDecimal executante = null;
		
		String PedidoSecosCongelados = (String) gc_solicitabast.getProperty("AD_TIPOPRODUTOS");
		
		if("1".equals(PedidoSecosCongelados)) { //secos
			
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
			
		}else {
			
			try {

				JdbcWrapper jdbcWrapper = null;
				EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
				jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
				ResultSet contagem;
				NativeSql nativeSql = new NativeSql(jdbcWrapper);
				nativeSql.resetSqlBuf();
				nativeSql.appendSql("select NVL(ABASTCONGELADOS,815) as ABASTCONGELADOS from ad_rotatel where id in (select id from ad_rotatelins where codbem='"+patrimonio+"') and nvl(ROTATELPROPRIA,'N')='S' and rownum=1");
				contagem = nativeSql.executeQuery();
				while (contagem.next()) {
					executante = contagem.getBigDecimal("ABASTCONGELADOS");
				}

			} catch (Exception e) {
				salvarException("[getAtendenteRota] Nao foi possibel o atendente da rota! patrimonio "+ patrimonio + e.getMessage() + "\n" + e.getCause());
			}
			
		}
		
		return executante;
	}
	
	private DynamicVO getADPATRIMONIO(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("PATRIMONIO");
		DynamicVO VO = DAO.findOne("CODBEM=?", new Object[] { patrimonio });
		return VO;
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
	
	private Timestamp addDias(Timestamp datainicial,BigDecimal prazo){
		GregorianCalendar gcm = new GregorianCalendar();
		Date data = new Date(datainicial.getTime());
		gcm.setTime(data);
		gcm.add(Calendar.DAY_OF_MONTH, prazo.intValue());
		data = gcm.getTime();
		Timestamp dataInicialMaisPrazo = new Timestamp(data.getTime());
		
		return dataInicialMaisPrazo;
	}
	
	private void geraItemOS(BigDecimal numos, String patrimonio, DynamicVO gc_solicitabast, BigDecimal sub) throws Exception{
		

		BigDecimal atendenteRota = null;
		
		if(sub!=null) {
			atendenteRota = sub;
		}else {
			atendenteRota = getAtendenteRota(patrimonio, gc_solicitabast);
		}

		BigDecimal motivo = new BigDecimal(111);
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
	
	private BigDecimal getContrato(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("PATRIMONIO");
		DynamicVO VO = DAO.findOne("CODBEM=?", new Object[] { patrimonio });
		BigDecimal contrato = VO.asBigDecimal("NUMCONTRATO");
		return contrato;
	}
	
	private BigDecimal getParceiro(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("PATRIMONIO");
		DynamicVO VO = DAO.findOne("CODBEM=?", new Object[] { patrimonio });
		BigDecimal contrato = VO.asBigDecimal("NUMCONTRATO");

		DAO = JapeFactory.dao("Contrato");
		DynamicVO VOS = DAO.findOne("NUMCONTRATO=?", new Object[] { contrato });
		BigDecimal parceiro = VOS.asBigDecimal("CODPARC");

		return parceiro;
	}
	
	private BigDecimal gerarCabecalhoOS(String patrimonio) throws Exception{
		
		String problema = "ABASTECER BEM: "+patrimonio;	
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
			NotaProdVO.setProperty("DESCRICAO",problema);
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
	
	public void totalizaImpostos(BigDecimal nunota) throws Exception{
        ImpostosHelpper impostos = new ImpostosHelpper();
        impostos.carregarNota(nunota);
        impostos.setForcarRecalculo(true);
        impostos.calcularTotalItens(nunota, true);
        impostos.totalizarNota(nunota);
        impostos.salvarNota();
	}
	
	private void salvaNumeroDaNota(BigDecimal nunota, String patrimonio, BigDecimal idSolicitacao, BigDecimal idRetorno) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("GCSolicitacoesAbastecimento","this.CODBEM=? AND this.ID=? ", new Object[] { patrimonio,idSolicitacao }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			DynamicVO VO = (DynamicVO) NVO;

			VO.setProperty("NUNOTA", nunota);

			itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			salvarException("[salvaNumeroDaNota] Nao foi possivel salvar o numero da nota! patrimonio "+patrimonio+" abastecimento novo."+e.getMessage()+"\n"+e.getCause()); 
		}
		
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_RETABAST","this.CODBEM=? AND this.ID=? ", new Object[] { patrimonio,idRetorno }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			DynamicVO VO = (DynamicVO) NVO;

			VO.setProperty("NUNOTA", nunota);

			itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			salvarException("[salvaNumeroDaNota] Nao foi possivel salvar o numero da nota! patrimonio "+patrimonio+" abastecimento novo."+e.getMessage()+"\n"+e.getCause()); 
		}
	}
	
	private DynamicVO getEnderecamento(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("ENDERECAMENTO");
		DynamicVO VO = DAO.findOne("CODBEM=?", new Object[] { patrimonio });
		return VO;
	}
	
	private BigDecimal getLocalAbast(String patrimonio) throws Exception {
		DynamicVO ad_enderecamento = getEnderecamento(patrimonio);
		BigDecimal local = null;
		
		//local de abast
		BigDecimal localAbast = (BigDecimal) ad_enderecamento.getProperty("CODLOCALABAST");
		  if(localAbast!=null) {
			  local = localAbast;
		  }else {
			  local = new BigDecimal(1110); //talvez transformar em parametro
		  }
		  
		  return local;
	}
	
	private DynamicVO getTcscon(BigDecimal contrato) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Contrato");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=?", new Object[] { contrato });
		return VO;
	}
	
	private BigDecimal getEmpresaAbast(String patrimonio, DynamicVO gc_solicitabast) throws Exception {
		
		String secosCongelados = (String) gc_solicitabast.getProperty("AD_TIPOPRODUTOS");
		DynamicVO ad_enderecamento = getEnderecamento(patrimonio); 
		BigDecimal empAbast = (BigDecimal) ad_enderecamento.getProperty("CODEMPABAST");
		BigDecimal empresaParaNota = null;
		
		BigDecimal contrato = getContrato(patrimonio); 
		DynamicVO tcscon = getTcscon(contrato);
		BigDecimal codemp = (BigDecimal) tcscon.getProperty("CODEMP"); 

		if(empAbast!=null) {
			  
			  if(empAbast.intValue()==1 && "1".equals(secosCongelados)) {
				  empresaParaNota = new BigDecimal(6);
			  }else if ((empAbast.intValue()==1 || empAbast.intValue()==6) && "2".equals(secosCongelados)) {
				  empresaParaNota = new BigDecimal(2);
			  }else {
				  empresaParaNota = empAbast;
			  }
			  	  
		  }else {
			  
			  if(codemp.intValue()==1 && "1".equals(secosCongelados)) {
				  empresaParaNota = new BigDecimal(6);
			  } else if ((codemp.intValue()==1 || codemp.intValue()==6) && "2".equals(secosCongelados)) {
				  empresaParaNota = new BigDecimal(2);
			  }else {
				  empresaParaNota = codemp;
			  }  
			  
		  }
		
		/*
		System.out.println(
				"*****************************"+
				"\nSecos Congelados: "+secosCongelados+
				"\nEmpresa Abast: "+empAbast+
				"\nEmpresa para Nota: "+empresaParaNota+
				"\nContrato: "+contrato+
				"\nEmpresa: "+codemp
				);
		*/
		return empresaParaNota;
	}
	
	private DynamicVO getAdConfigTelPro(BigDecimal empresa, BigDecimal local) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_CONFIGTELPRO");
		DynamicVO VO = DAO.findOne("CODEMP=? AND CODLOCAL=?", new Object[] { empresa, local });
		return VO;
	}
	
	private BigDecimal getTop(BigDecimal empresaParaNota, BigDecimal local) throws Exception {
		DynamicVO adConfigTelPro = getAdConfigTelPro(empresaParaNota, local);
		BigDecimal top = null;

		BigDecimal topTelaConfig = null;

		if (adConfigTelPro != null) {
			topTelaConfig = (BigDecimal) adConfigTelPro.getProperty("CODTIPOPER");
		}

		if (topTelaConfig != null) {
			top = topTelaConfig;
		} else {
			top = new BigDecimal(1018); // talvez transformar em parametro
		}

		return top;
	}
	
	private DynamicVO getTGFPRO(BigDecimal produto) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?", new Object[] { produto });
		return VO;
	}
	
	private BigDecimal validaQtdMinDoItem(BigDecimal produto) throws Exception {
		BigDecimal ad_qtdmin = getTGFPRO(produto).asBigDecimal("AD_QTDMIN");
		BigDecimal qtdMinima = null;
		if(ad_qtdmin!=null) {
			qtdMinima = ad_qtdmin;
		}else {
			qtdMinima = new BigDecimal(1);
		}
		
		return qtdMinima;
	}
	
	private BigDecimal getEstoqueDoItem(BigDecimal empresa, BigDecimal local, BigDecimal produto) {
		
		BigDecimal estoque = BigDecimal.ZERO;
		String controle = " ";
		String ativo = "S";
		BigDecimal codparc = BigDecimal.ZERO;
		
		try {
			JapeWrapper DAO = JapeFactory.dao("Estoque");
			DynamicVO VO = DAO.findOne("CODEMP=? and CODLOCAL=? and CODPROD=? and CONTROLE=? and ATIVO=? and CODPARC=?",new Object[] { empresa, local,produto,controle,ativo,codparc});
			if(VO!=null) {
				BigDecimal estoqueNaEmpresa = VO.asBigDecimal("ESTOQUE");
				BigDecimal reservado = VO.asBigDecimal("RESERVADO");
				
				estoque = estoqueNaEmpresa.subtract(reservado);
				
			}
		} catch (Exception e) {
			salvarException(
					"[getEstoqueDoItem] Nao foi possivel obter o estoque do item! Empresa "+empresa+" local "+local+" produto "+produto
							+ e.getMessage() + "\n" + e.getCause());
		}
		
		return estoque;
	}
	
	private void identificaItens(BigDecimal nunota, String patrimonio, DynamicVO gc_solicitabast) {
		try {
			
			BigDecimal localAbast = getLocalAbast(patrimonio);
			BigDecimal empresaAbast = getEmpresaAbast(patrimonio, gc_solicitabast);
			BigDecimal top = getTop(empresaAbast, localAbast);
			int sequencia = 0;
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("GCPlanograma","this.CODBEM = ? and this.AD_ABASTECER=?", new Object[] { patrimonio, "S" }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			//BigDecimal capacidade = (BigDecimal) DynamicVO.getProperty("CAPACIDADE");
			//BigDecimal nivelalerta = (BigDecimal) DynamicVO.getProperty("NIVELALERTA");
			
			String tecla = (String) DynamicVO.getProperty("TECLA");
			BigDecimal produto = (BigDecimal) DynamicVO.getProperty("CODPROD");
			BigDecimal nivelpar = (BigDecimal) DynamicVO.getProperty("NIVELPAR");
			BigDecimal vlrpar = (BigDecimal) DynamicVO.getProperty("VLRPAR");
			BigDecimal vlrfun = (BigDecimal) DynamicVO.getProperty("VLRFUN");
			String volume = getTGFPRO(produto).asString("CODVOL");
			
			//estoque do item
			BigDecimal estoque = validaEstoqueDoItem(DynamicVO.asBigDecimal("ESTOQUE"));
			
			BigDecimal falta = nivelpar.subtract(estoque);
			BigDecimal valor = vlrpar.add(vlrfun);
			BigDecimal estoqueNaEmpresa = getEstoqueDoItem(empresaAbast,localAbast,produto);

			//qtd minima para abastecimento
			BigDecimal qtdMinima = validaQtdMinDoItem(produto);
			
			//valor total
			BigDecimal valorTotal = falta.multiply(valor);
			
			//validacao
			if(falta.doubleValue() % qtdMinima.doubleValue() == 0) {
				if(falta.intValue() <= estoqueNaEmpresa.intValue()) {
					if(falta.intValue()>0) {
						sequencia++;
						insereItemNaNota(nunota, empresaAbast, localAbast, produto, volume, falta, new BigDecimal(sequencia), valorTotal, valor, tecla, top, gc_solicitabast);
					}
				}else {
					//TODO :: insere itens em corte / ruptura
					insereItemEmRuptura(nunota, empresaAbast, localAbast, produto, falta, valor, tecla, gc_solicitabast, patrimonio);
				}
			}
			
			}
		} catch (Exception e) {
			salvarException(
					"[identificaItens] Nao foi possivel identificar os itens! patrimonio "+patrimonio
							+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	
	private DynamicVO getTGFGRU(BigDecimal grupo) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("GrupoProduto");
		DynamicVO VO = DAO.findOne("CODGRUPOPROD=?", new Object[] { grupo });
		return VO;
	}
	
	private String validaSeOhItemEhDeCongelados(BigDecimal produto) throws Exception {
		String congelado = "";
		
		DynamicVO tgfpro = getTGFPRO(produto);
		if(tgfpro!=null) {
			BigDecimal codgrupo = tgfpro.asBigDecimal("CODGRUPOPROD");
			
			if(codgrupo!=null) {
				DynamicVO tgfgru = getTGFGRU(codgrupo);
				if(tgfgru!=null) {
					congelado = tgfgru.asString("AD_CONGELADOS");
				}
			}	
			
		}
		
		if(congelado=="" || congelado==null) {
			congelado="N";
		}
		
		return congelado;
	}
	
	private void insereItemNaNota(BigDecimal nunota, BigDecimal empresa, BigDecimal local, BigDecimal produto, 
			String volume, BigDecimal qtdneg, BigDecimal sequencia, BigDecimal vlrtot, BigDecimal vlrunit, String tecla, BigDecimal top, DynamicVO gc_solicitabast) {
		try {
			
			String PedidoSecosCongelados = (String) gc_solicitabast.getProperty("AD_TIPOPRODUTOS");
			String tipoAbastecimento = "";
			
			if("1".equals(PedidoSecosCongelados)) {
				tipoAbastecimento="N";
			}else {
				tipoAbastecimento="S";
			}
			
			String validaSeOhItemEhDeCongelados = validaSeOhItemEhDeCongelados(produto);
			
			if(tipoAbastecimento.equals(validaSeOhItemEhDeCongelados)) {
				
				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("ItemNota");
				DynamicVO VO = (DynamicVO) NPVO;
				
				VO.setProperty("NUNOTA", nunota);
				VO.setProperty("CODEMP", empresa);
				VO.setProperty("CODLOCALORIG", local);
				VO.setProperty("CODPROD", produto);
				VO.setProperty("CODVOL", volume);
				VO.setProperty("QTDNEG", qtdneg);
				VO.setProperty("SEQUENCIA", sequencia);
				VO.setProperty("VLRTOT", vlrtot);
				VO.setProperty("VLRUNIT", vlrunit);
				VO.setProperty("RESERVA", validaReserva(top));
				VO.setProperty("ATUALESTOQUE", new BigDecimal(validaAtualEstoque(top)));
				VO.setProperty("AD_TECLA", tecla);
				
				dwfFacade.createEntity("ItemNota", (EntityVO) VO);
			}
			
			
		} catch (Exception e) {
			salvarException(
					"[insereItemNaNota] Nao foi possivel inserir o item na nota! numero nota "+nunota+" produto "+produto
							+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void insereItemEmRuptura(BigDecimal nunota, BigDecimal empresa, BigDecimal local, BigDecimal produto, 
			BigDecimal qtdneg, BigDecimal vlrunit, String tecla, DynamicVO gc_solicitabast, String patrimonio) {
		try {
			
			String PedidoSecosCongelados = (String) gc_solicitabast.getProperty("AD_TIPOPRODUTOS");
			String tipoAbastecimento = "";
			
			if("1".equals(PedidoSecosCongelados)) {
				tipoAbastecimento="N";
			}else {
				tipoAbastecimento="S";
			}
			
			String validaSeOhItemEhDeCongelados = validaSeOhItemEhDeCongelados(produto);
			
			if(tipoAbastecimento.equals(validaSeOhItemEhDeCongelados)) {
				
				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_ITENSCORTE");
				DynamicVO VO = (DynamicVO) NPVO;
				
				VO.setProperty("NUNOTA", nunota);
				VO.setProperty("CODPROD", produto);
				VO.setProperty("TECLA", tecla);
				VO.setProperty("CODBEM", patrimonio);
				VO.setProperty("CODEMP", empresa);
				VO.setProperty("CODLOCALORIG", local);
				VO.setProperty("QTDNEG", qtdneg);
				VO.setProperty("VLRUNIT", vlrunit);

				dwfFacade.createEntity("AD_ITENSCORTE", (EntityVO) VO);
			}
			
			
		} catch (Exception e) {
			salvarException(
					"[insereItemEmRuptura] Nao foi possivel inserir o item em ruptura! numero nota "+nunota+" produto "+produto
							+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private int validaAtualEstoque(BigDecimal top) {
		int atualestoque = 0;
		
		if(top.intValue()==1018) {
			atualestoque=1;
		}else {
			atualestoque=-1;
		}
		
		return atualestoque;
	}
	
	private String validaReserva(BigDecimal top) {
		
		String reserva = "";
		
		if(top.intValue()==1018) {
			reserva="S";
		}else {
			reserva="N";
		}
		return reserva;
	}
	
	private BigDecimal geraCabecalho(String patrimonio, DynamicVO gc_solicitabast) throws Exception {
		
		  Timestamp dataAgendamento = (Timestamp) gc_solicitabast.getProperty("DTAGENDAMENTO");
		  //int compareTo = dataAgendamento.compareTo(TimeUtils.getNow());
		  BigDecimal nunota = null;
		  
		  try { 
			 
			  DynamicVO ad_enderecamento = getEnderecamento(patrimonio); 
			  BigDecimal contrato = getContrato(patrimonio); 
			  String secosCongelados = (String) gc_solicitabast.getProperty("AD_TIPOPRODUTOS"); //1=secos, 2=congelados
			  
			  // dados para a nota
			  BigDecimal empresaParaNota = getEmpresaAbast(patrimonio, gc_solicitabast);
			  BigDecimal local = getLocalAbast(patrimonio);
			  BigDecimal top = getTop(empresaParaNota,local);
			  Timestamp dtneg = dataAgendamento;
			  BigDecimal codparc = getParceiroEmpresa(empresaParaNota);
			  BigDecimal codlocal = null;
			  BigDecimal numcontrato = contrato;
			  String descricao = "CODBEM : "+patrimonio;
			  BigDecimal codusuinc = new BigDecimal(3082);
			  				 
			  
			  //descobre o código local da nota
			  BigDecimal idPlanta = (BigDecimal) ad_enderecamento.getProperty("ID");
			  DynamicVO adPlantas = getAdPlantas(contrato, idPlanta);
			  BigDecimal localPla = (BigDecimal) adPlantas.getProperty("CODLOCAL");
			  
			  if("2".equals(secosCongelados)) {
				  codlocal = new BigDecimal(2099);
			  }else {
				  codlocal = localPla;
			  }
			  
			  BigDecimal nuNotaModelo = new BigDecimal(134966926);
				 
				  EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade(); 
				  EntityVO padraoNPVO = dwfFacade.getDefaultValueObjectInstance(DynamicEntityNames.CABECALHO_NOTA);
				  DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("CabecalhoNota", nuNotaModelo);
				  DynamicVO NotaProdVO = (DynamicVO) padraoNPVO;
			  
				  DynamicVO topRVO = ComercialUtils.getTipoOperacao(top); 
				  String tipoMovimento = topRVO.asString("TIPMOV");
				  
				  NotaProdVO.setProperty("CODEMP", empresaParaNota); //ok
				  NotaProdVO.setProperty("CODTIPOPER", top); //ok
				  NotaProdVO.setProperty("TIPMOV", tipoMovimento);
				  NotaProdVO.setProperty("SERIENOTA", ModeloNPVO.asString("SERIENOTA"));
				  NotaProdVO.setProperty("CODPARC", codparc);
				  NotaProdVO.setProperty("NUMCONTRATO", numcontrato);
				  NotaProdVO.setProperty("CODTIPVENDA", ModeloNPVO.asBigDecimal("CODTIPVENDA")); 
				  NotaProdVO.setProperty("CODNAT", ModeloNPVO.asBigDecimal("CODNAT"));
				  NotaProdVO.setProperty("CODCENCUS", ModeloNPVO.asBigDecimal("CODCENCUS"));
				  NotaProdVO.setProperty("NUMNOTA", new java.math.BigDecimal(0));
				  NotaProdVO.setProperty("APROVADO", ModeloNPVO.asString("APROVADO"));
				  NotaProdVO.setProperty("PENDENTE", "S"); 
				  NotaProdVO.setProperty("CIF_FOB", ModeloNPVO.asString("CIF_FOB")); 
				  NotaProdVO.setProperty("DTNEG", dtneg);
				  NotaProdVO.setProperty("AD_CODLOCAL", codlocal);
				  NotaProdVO.setProperty("AD_CODBEM", patrimonio);
				  NotaProdVO.setProperty("OBSERVACAO", descricao);
				  NotaProdVO.setProperty("CODVEND", ModeloNPVO.asBigDecimal("CODVEND"));
				  NotaProdVO.setProperty("CODUSUINC", codusuinc);
				  NotaProdVO.setProperty("CODEMPNEGOC", empresaParaNota); 
				  NotaProdVO.setProperty("TIPFRETE", "N");
	
				  dwfFacade.createEntity(DynamicEntityNames.CABECALHO_NOTA, (EntityVO) NotaProdVO); 
				  nunota = NotaProdVO.asBigDecimal("NUNOTA");

			  } catch (Exception e) {
			  salvarException("[geraCabecalho] Nao foi possivel gerar cabecalho! patrimonio "+patrimonio+" abastecimento novo."+e.getMessage()+"\n"+e.getCause()); 
			  }   	

		  return nunota;
	}
	
	private BigDecimal getParceiroEmpresa(BigDecimal empresa) throws Exception {
		BigDecimal codparceiro = null;
		JapeWrapper DAO = JapeFactory.dao("Empresa");
		DynamicVO VO = DAO.findOne("CODEMP=?", new Object[] { empresa});
		if(VO!=null) {
			BigDecimal parceirodaempresa = VO.asBigDecimal("CODPARC");
			if(parceirodaempresa!=null) {
				codparceiro = parceirodaempresa;
			}
		}
		
		if(codparceiro==null) {
			codparceiro = new BigDecimal(2);
		}
		return codparceiro;
	}
	
	private DynamicVO getAdPlantas(BigDecimal contrato, BigDecimal id) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("PLANTAS");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=? AND ID=?", new Object[] { contrato, id });
		return VO;
	}
	
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "acaoAgendada_geravisita");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
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
