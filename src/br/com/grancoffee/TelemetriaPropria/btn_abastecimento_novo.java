package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import com.sankhya.util.TimeUtils;
//import Helpers.WSPentaho;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
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
//import br.com.sankhya.modelcore.util.MGECoreParameter;
import br.com.sankhya.ws.ServiceContext;

public class btn_abastecimento_novo implements AcaoRotinaJava {

	/**
	 * 15/10/2021 vs 1.0 Botao para gerar o abastecimento, al�m disso realizar� a gera��o da nota e da OS de visita.
	 * 23/10/2021 vs 1.1 Inserido m�todo insereItemEmRuptura para inserir os itens que precisavam ser abastecidos por�m n�o tinha a quantidade em estoque
	 * 03/11/2021 vs 1.2 Ajustado o m�todo validaPedido estava permitindo a cria��o de 2 pedidos de congelados, estava errado o Where da segunda valida��o ANTES: NVL(AD_TIPOPRODUTOS,'1')='1' DEPOIS: NVL(AD_TIPOPRODUTOS,'1')='2'
	 * 08/11/2021 vs 1.3 Inserido a valida��o para n�o gerar o pedido se a m�quina esta totalmente vazia e houve um pedido de abastecimento nos �ltimos 15 dias.
	 * 24/11/2021 vs 1.5 Ajustado a gera��o dos pedidos considerando a quantidade m�nima.
	 */
	
	String retornoNegativo = "";
	int cont = 0;

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();

		start(linhas, arg0);

		if (cont > 0) {
			arg0.setMensagemRetorno("Foram solicitado(s) <b>" + cont + "</b> abastecimento(s)!");
		}
	}

	private void start(Registro[] linhas, ContextoAcao arg0) throws Exception {

		String tipoAbastecimento = (String) arg0.getParam("TIPABAST");// 1=Agora 2=Agendado
		String secosCongelados = (String) arg0.getParam("SECOSECONGELADOS");// 1=Abastecer Apenas Secos.2=Abastecer Apenas Congelados.3=Abastecer Secos e Congelados
		DynamicVO gc_solicitabast = null;
		BigDecimal idRetorno = null;

		for (int i = 0; i < linhas.length; i++) {
			
			//TODO :: Verificar se o estoque est� todo zerado, se tiver fazer a pergunta para o usu�rio, se ele quer de fato continuar...
			boolean maquinaDesabastecida = verificarSeAhMaquinaEstaTotalmenteDesabastecida(linhas[i].getCampo("CODBEM").toString());
			boolean confirmarSimNao = true;
			
			if(maquinaDesabastecida) {
				confirmarSimNao = arg0.confirmarSimNao("Aten��o", "A m�quina est� totalmente desabastecida, talvez seja necess�rio aguardar um pouco at� a rotina atualizar o estoque! caso n�o normalize nos pr�ximos 10 minutos, acionar o setor de T.I, </br> deseja abastecer mesmo assim ?", 1);
			}
			
			if(confirmarSimNao) {
				
				BigDecimal idflow = (BigDecimal) linhas[i].getCampo("AD_IDFLOW");
				Timestamp dtAbastecimento = validacoes(linhas[i], arg0, tipoAbastecimento, secosCongelados);

				if ("1".equals(secosCongelados)) { // apenas secos
					idRetorno = cadastrarNovoAbastecimento(linhas[i].getCampo("CODBEM").toString(), "S",
							"N", idflow);// salva tela Abastecimento

					if (idRetorno != null) {
						apenasSecos(idRetorno, dtAbastecimento, linhas[i].getCampo("CODBEM").toString());// carrega
																												// itens

						if (dtAbastecimento != null) {// agendado
							gc_solicitabast = agendarAbastecimento(linhas[i].getCampo("CODBEM").toString(), TimeUtils.getNow(),
									dtAbastecimento, idRetorno, "S", "N");
						} else {// agora
							gc_solicitabast = agendarAbastecimento(linhas[i].getCampo("CODBEM").toString(), TimeUtils.getNow(),
									TimeUtils.getNow(), idRetorno, "S", "N");
							
							gerarPedidoENota(linhas[i], gc_solicitabast, arg0, idRetorno);
						}
						cont++;
						
					}	
				}

				else if ("2".equals(secosCongelados)) {// apenas congelados
					idRetorno = cadastrarNovoAbastecimento(linhas[i].getCampo("CODBEM").toString(), "N",
							"S", idflow);

					if (idRetorno != null) {
						apenasCongelados(idRetorno, dtAbastecimento, linhas[i].getCampo("CODBEM").toString());// carrega
																													// itens

						if (dtAbastecimento != null) {// agendado
							gc_solicitabast = agendarAbastecimento(linhas[i].getCampo("CODBEM").toString(), TimeUtils.getNow(),
									dtAbastecimento, idRetorno, "N", "S");
						} else {// agora
							gc_solicitabast = agendarAbastecimento(linhas[i].getCampo("CODBEM").toString(), TimeUtils.getNow(),
									TimeUtils.getNow(), idRetorno, "N", "S");
							
							gerarPedidoENota(linhas[i], gc_solicitabast, arg0, idRetorno);
						}
						cont++;
						
					}
				}

				else { // secos e congelados
					BigDecimal idSecos = cadastrarNovoAbastecimento(linhas[i].getCampo("CODBEM").toString(), "S", "N",
							idflow);
					if (idSecos != null) {
						apenasSecos(idSecos, dtAbastecimento, linhas[i].getCampo("CODBEM").toString());
						if (dtAbastecimento != null) {// agendado
							gc_solicitabast = agendarAbastecimento(linhas[i].getCampo("CODBEM").toString(), TimeUtils.getNow(),
									dtAbastecimento, idSecos, "S", "N");
						} else {// agora
							gc_solicitabast = agendarAbastecimento(linhas[i].getCampo("CODBEM").toString(), TimeUtils.getNow(),
									TimeUtils.getNow(), idSecos, "S", "N");
							
							gerarPedidoENota(linhas[i], gc_solicitabast, arg0, idSecos);
						}
						cont++;
						
					}

					BigDecimal idcongelados = cadastrarNovoAbastecimento(linhas[i].getCampo("CODBEM").toString(), "N", "S",
							idflow);
					if (idcongelados != null) {
						apenasCongelados(idcongelados, dtAbastecimento, linhas[i].getCampo("CODBEM").toString());
						if (dtAbastecimento != null) {// agendado
							gc_solicitabast = agendarAbastecimento(linhas[i].getCampo("CODBEM").toString(), TimeUtils.getNow(),
									dtAbastecimento, idcongelados, "N", "S");
						} else {// agora
							gc_solicitabast = agendarAbastecimento(linhas[i].getCampo("CODBEM").toString(), TimeUtils.getNow(),
									TimeUtils.getNow(), idcongelados, "N", "S");
							
							gerarPedidoENota(linhas[i], gc_solicitabast, arg0, idcongelados);
						}
						cont++;
						
					}
				}

				linhas[i].setCampo("AD_IDFLOW", null);
				
			}

			linhas[i].setCampo("AD_IDFLOW", null);
			linhas[i].setCampo("PLANOGRAMAPENDENTE", "S");

		}
		//chamaPentaho();
	}
	
	private void gerarPedidoENota(Registro linhas, DynamicVO gc_solicitabast, ContextoAcao arg0, BigDecimal idRetorno) throws Exception {
		BigDecimal nunota = null;
		
		nunota = geraCabecalho(linhas, gc_solicitabast);
		if(nunota!=null) {
			
			identificaItens(nunota,linhas.getCampo("CODBEM").toString(),gc_solicitabast);
			salvaNumeroDaNota(nunota, linhas.getCampo("CODBEM").toString(), gc_solicitabast.asBigDecimal("ID"), idRetorno);
			totalizaImpostos(nunota);
			BigDecimal numos = gerarCabecalhoOS(linhas.getCampo("CODBEM").toString());
			
			if(numos!=null) {
				geraItemOS(numos, linhas.getCampo("CODBEM").toString(), gc_solicitabast);
				salvaNumeroOS(numos, linhas.getCampo("CODBEM").toString(), gc_solicitabast.asBigDecimal("ID"), idRetorno, gc_solicitabast);
				
				verificaPlanogramaPendente(linhas.getCampo("CODBEM").toString(), numos, nunota, gc_solicitabast.asBigDecimal("ID"), idRetorno);
				
				validaAD_TROCADEGRADE(linhas.getCampo("CODBEM").toString(), numos, nunota);
				validaItensDaTrocaDeGrade(linhas.getCampo("CODBEM").toString(), numos);
			}

		}else {
			arg0.mostraErro("<b> OPS! algo deu errado! acionar o setor de T.I! </b>");
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
				
				//TODO :: Para visita de secos, deve ser retirado apenas os secos e para congelados os congelados.
				//?? pendente
			}

			}
		} catch (Exception e) {
			salvarException("[validaItensDaTrocaDeGrade] Nao foi possivel verificar os produtos a serem retirados " + patrimonio
					+ e.getMessage() + "\n" + e.getCause());
		}
		
		//TODO :: verificar itens novos e j� existentes
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
			}else { //produto j� existe
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
			VO.setProperty("QTDRET", new BigDecimal(0));
			
			dwfFacade.createEntity("AD_TROCADEGRADE", (EntityVO) VO);
			
		} catch (Exception e) {
			salvarException("[validaTeclasGC_PLANOGRAMA] Nao foi possivel inserir na AD_TROCADEGRADE " + patrimonio
					+ e.getMessage() + "\n" + e.getCause());	 
		}
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
	
	private BigDecimal gerarCabecalhoOS(String patrimonio) throws Exception{
		
		BigDecimal numos = null;
		
		
		try {
			
			String problema = "ABASTECER BEM: "+patrimonio;	
			BigDecimal parceiro = getParceiro(patrimonio);
			BigDecimal contrato = getContrato(patrimonio);
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("OrdemServico",new BigDecimal(593595));
			DynamicVO NotaProdVO = ModeloNPVO.buildClone();
			
			//BigDecimal usuario = getUsuLogado();

			NotaProdVO.setProperty("DHCHAMADA", TimeUtils.getNow());
			NotaProdVO.setProperty("DTPREVISTA",addDias(TimeUtils.getNow(),new BigDecimal(7)));
			NotaProdVO.setProperty("NUMOS",null); 
			NotaProdVO.setProperty("SITUACAO","P");
			NotaProdVO.setProperty("CODUSUSOLICITANTE",new BigDecimal(3082));
			NotaProdVO.setProperty("CODUSURESP",new BigDecimal(3082));
			NotaProdVO.setProperty("DESCRICAO",problema);
			NotaProdVO.setProperty("AD_MANPREVENTIVA", "N");
			NotaProdVO.setProperty("AD_CHAMADOTI", "N");
			NotaProdVO.setProperty("AD_TELPROPRIA", "S");
			NotaProdVO.setProperty("CODATEND", new BigDecimal(3082));
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
					"[gerarCabecalhoOS] Nao foi possivel Gerar o cabe�alho da OS! Patrimonio "+patrimonio
							+ e.getMessage() + "\n" + e.getCause());
		}
		
		return numos;
		
	}
	
	
	private void geraItemOS(BigDecimal numos, String patrimonio, DynamicVO gc_solicitabast) throws Exception{
		

		BigDecimal atendenteRota = getAtendenteRota(patrimonio, gc_solicitabast);
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
	
	private boolean validaSeOhPedidoDeAbastecimentoPoderaSerGerado(String secosCongelados, String codbem) {
		
		boolean valida = false;
		
		try {
			
			String tipoAbastecimento = "";
			
			if("1".equals(secosCongelados)) {
				tipoAbastecimento="N";
			}else {
				tipoAbastecimento="S";
			}
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
			"SELECT COUNT(*) AS QTD "+
			"FROM("+
				"SELECT X.CODBEM, X.CODPROD, X.FALTA, X.EMP_ABAST, X.LOCAL_ABAST, AD_CONGELADOS FROM("+
					"SELECT T.CODBEM, P.CODPROD,(P.NIVELPAR-P.ESTOQUE) AS FALTA, "+
					"CASE WHEN NVL(EO.CODEMPABAST,C.CODEMP)=1 AND '"+secosCongelados+"'='1' THEN 6 WHEN NVL(EO.CODEMPABAST,C.CODEMP) IN (1,6) AND '"+secosCongelados+"'='2' THEN 2 WHEN NVL(EO.CODEMPABAST,C.CODEMP)<>1 THEN NVL(EO.CODEMPABAST,C.CODEMP) END AS EMP_ABAST, "+
					"NVL(EO.CODLOCALABAST,1110) AS LOCAL_ABAST, NVL(GRU.AD_CONGELADOS,'N') AS AD_CONGELADOS, NVL(PRO.AD_QTDMIN,1) AS AD_QTDMIN FROM GC_INSTALACAO T "+
					"JOIN GC_PLANOGRAMA P ON (P.CODBEM=T.CODBEM) JOIN AD_ENDERECAMENTO EO ON (EO.CODBEM=T.CODBEM) JOIN TCSCON C ON (C.NUMCONTRATO=EO.NUMCONTRATO) JOIN TGFPRO PRO ON (PRO.CODPROD=P.CODPROD) JOIN TGFGRU GRU ON (GRU.CODGRUPOPROD=PRO.CODGRUPOPROD) "+
					"WHERE T.CODBEM='"+codbem+"' AND P.AD_ABASTECER='S' AND (P.NIVELPAR - P.ESTOQUE)> 0 "+
				") X "+
				"LEFT JOIN TGFEST E ON (E.CODEMP=X.EMP_ABAST AND E.CODLOCAL=X.LOCAL_ABAST AND E.CODPROD=X.CODPROD AND E.CONTROLE=' ' AND E.ATIVO='S' AND E.CODPARC=0) "+
				"WHERE (E.ESTOQUE - E.RESERVADO) >= FALTA AND '"+tipoAbastecimento+"' = AD_CONGELADOS AND TRUNC(FALTA/AD_QTDMIN)>0 "+
			")");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("QTD");
				if (count >= 1) {
					valida = true;
				}
			}

		} catch (Exception e) {
			salvarException(
					"[validaSeOhPedidoDeAbastecimentoPoderaSerGerado] Nao foi possivel validar a quantidade de itens! Patrimonio "+codbem
							+ e.getMessage() + "\n" + e.getCause());
		}
		
		return valida;
		
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
			//if(falta.divide(qtdMinima, 2, RoundingMode.HALF_EVEN).doubleValue()>0) {
			
			if(falta.doubleValue() <= estoqueNaEmpresa.doubleValue() && falta.doubleValue()>0) {
				
				if(qtdMinima.doubleValue()>1) { //possui qtd minima
					
					if(falta.doubleValue()>qtdMinima.intValue()) {
						BigDecimal qtdVezes = falta.divide(qtdMinima, 0, RoundingMode.HALF_EVEN);
						BigDecimal qtdParaNota = qtdVezes.multiply(qtdMinima);
						
						if(qtdParaNota.doubleValue()<=nivelpar.doubleValue()) {
							sequencia++;
							insereItemNaNota(nunota, empresaAbast, localAbast, produto, volume, qtdParaNota, new BigDecimal(sequencia), valorTotal, valor, tecla, top, gc_solicitabast);
						}else { //quantidade para nota, a cima do n�vel par.
							//cortado
							insereItemEmRuptura(nunota, empresaAbast, localAbast, produto, volume, falta, new BigDecimal(sequencia), valorTotal, valor, tecla, top, gc_solicitabast, patrimonio, "Falta "+falta+" quantidade para nota "+qtdParaNota+" n�vel par "+nivelpar+", quantidade para a nota superior ao n�vel par.");
						}
								
					}else { //n atingiu a qtd minima
						//cortado
						insereItemEmRuptura(nunota, empresaAbast, localAbast, produto, volume, falta, new BigDecimal(sequencia), valorTotal, valor, tecla, top, gc_solicitabast, patrimonio, "Produto n�o atingiu a quantidade m�nima de "+qtdMinima+" itens.");
					}
					
				}else { //n�o possui qtd m�nima, pode inserir direto
					sequencia++;
					insereItemNaNota(nunota, empresaAbast, localAbast, produto, volume, falta, new BigDecimal(sequencia), valorTotal, valor, tecla, top, gc_solicitabast);
				}
				
			}else {
				//cortado
				insereItemEmRuptura(nunota, empresaAbast, localAbast, produto, volume, falta, new BigDecimal(sequencia), valorTotal, valor, tecla, top, gc_solicitabast, patrimonio, "Ruptura na filial");
			}
			
			
			/*
			 * if(falta.doubleValue() % qtdMinima.doubleValue() == 0) { if(falta.intValue()
			 * <= estoqueNaEmpresa.intValue()) { if(falta.intValue()>0) { sequencia++;
			 * insereItemNaNota(nunota, empresaAbast, localAbast, produto, volume, falta,
			 * new BigDecimal(sequencia), valorTotal, valor, tecla, top, gc_solicitabast); }
			 * }else { //TODO :: registra itens em ruptura insereItemEmRuptura(nunota,
			 * empresaAbast, localAbast, produto, volume, falta, new BigDecimal(sequencia),
			 * valorTotal, valor, tecla, top, gc_solicitabast, patrimonio); } }
			 */
			
			
			}
		} catch (Exception e) {
			salvarException(
					"[identificaItens] Nao foi possivel identificar os itens! patrimonio "+patrimonio
							+ e.getMessage() + "\n" + e.getCause());
		}
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
			String volume, BigDecimal qtdneg, BigDecimal sequencia, BigDecimal vlrtot, BigDecimal vlrunit, String tecla, BigDecimal top, DynamicVO gc_solicitabast, String patrimonio, String motivo) {
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
				VO.setProperty("MOTIVO", motivo);

				dwfFacade.createEntity("AD_ITENSCORTE", (EntityVO) VO);
			}
			
			
		} catch (Exception e) {
			salvarException(
					"[insereItemEmRuptura] Nao foi possivel inserir o item em ruptura! numero nota "+nunota+" produto "+produto
							+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	public void totalizaImpostos(BigDecimal nunota) throws Exception{
        ImpostosHelpper impostos = new ImpostosHelpper();
        impostos.carregarNota(nunota);
        impostos.setForcarRecalculo(true);
        impostos.calcularTotalItens(nunota, true);
        impostos.totalizarNota(nunota);
        impostos.salvarNota();
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
	
	private int validaAtualEstoque(BigDecimal top) {
		int atualestoque = 0;
		
		if(top.intValue()==1018) {
			atualestoque=1;
		}else {
			atualestoque=-1;
		}
		
		return atualestoque;
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
	
	private BigDecimal validaEstoqueDoItem(BigDecimal est) {
		BigDecimal estoque = null;
		if(est!=null) {
			estoque = est;
		}else {
			estoque = new BigDecimal(0);
		}
		return estoque;
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
	
	private void salvaNumeroOS(BigDecimal numos, String patrimonio, BigDecimal idSolicitacao, BigDecimal idRetorno, DynamicVO gc_solicitabast) {
		
		BigDecimal atendenteRota = getAtendenteRota(patrimonio, gc_solicitabast);
		
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
			salvarException("[salvaNumeroDaNota] Nao foi possivel salvar o numero da OS! patrimonio "+patrimonio+" OS: "+numos+" id solicita��o: "+idSolicitacao+" idretorno: "+idRetorno+" abastecimento novo."+e.getMessage()+"\n"+e.getCause()); 
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
			salvarException("[salvaNumeroDaNota] Nao foi possivel salvar o numero da OS! patrimonio "+patrimonio+" OS: "+numos+" id solicita��o: "+idSolicitacao+" idretorno: "+idRetorno+" abastecimento novo."+e.getMessage()+"\n"+e.getCause()); 
		}
			
		
	}
	
	private BigDecimal geraCabecalho(Registro linhas, DynamicVO gc_solicitabast) throws Exception {
		
		  Timestamp dataAgendamento = (Timestamp) gc_solicitabast.getProperty("DTAGENDAMENTO");
		  int compareTo = dataAgendamento.compareTo(TimeUtils.getNow());
		  BigDecimal nunota = null;
		  
		  
		  if(compareTo<=0) { //a data do agendamento � menor ou igual a data atual, se n�o for deve ser gerada por um agendamento.
			 
			  String patrimonio = (String) linhas.getCampo("CODBEM"); 
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
			  				 
			  
			  //descobre o c�digo local da nota
			  BigDecimal idPlanta = (BigDecimal) ad_enderecamento.getProperty("ID");
			  DynamicVO adPlantas = getAdPlantas(contrato, idPlanta);
			  BigDecimal localPla = (BigDecimal) adPlantas.getProperty("CODLOCAL");
			  
			  if("2".equals(secosCongelados)) {
				  codlocal = new BigDecimal(2099);
			  }else {
				  codlocal = localPla;
			  }
			  
			  BigDecimal nuNotaModelo = new BigDecimal(134966926);
			  
			  try { 
				 
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
	
	private boolean validaSeAhMaquinaEstaNaRota(String patrimonio) throws Exception {
		boolean valida = false;
		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT CASE WHEN EXISTS(SELECT CODBEM FROM AD_ROTATELINS WHERE CODBEM='" + patrimonio
					+ "') THEN 'S' ELSE 'N' END AS VALIDA FROM DUAL");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				String verifica = contagem.getString("VALIDA");
				if ("S".equals(verifica)) {
					valida = true;
				}
			}

		} catch (Exception e) {
			salvarException("[validaSeAhMaquinaEstaNaRota] N�o foi possivel verificar se a maquina " + patrimonio
					+ " esta na rota. " + e.getMessage() + "\n" + e.getCause());
		}
		return valida;
	}

	private Timestamp validacoes(Registro linhas, ContextoAcao arg0, String tipoAbastecimento, String secosCongelados)
			throws Exception {
		
		boolean maquinaNaRota = validaSeAhMaquinaEstaNaRota(linhas.getCampo("CODBEM").toString());

		if (!maquinaNaRota) {
			throw new Error("O patrim�nio " + linhas.getCampo("CODBEM").toString()
					+ " n�o est� em rota, n�o pode ser gerado o abastecimento!");
		}
		
		String tp = "";
		
		if("1".equals(secosCongelados)) {
			tp="Secos";
		}else {
			tp="Congelados";
		}
		
		if("3".equals(secosCongelados)) {
			
			String valid="";
			
			for(int x=1; x<=2; x++) {
				
				if(x==1) {
					valid = "1";
				}else {
					valid = "2";
				}
				
				if(!validaSeOhPedidoDeAbastecimentoPoderaSerGerado(valid,linhas.getCampo("CODBEM").toString())) {
					throw new Error("<b> Aten��o </b><br/><br/>"+
				" O pedido de <b>Secos</b> ou <b>Congelados</b> n�o pode ser gerado !<br/>"+
				" causas poss�veis: <br/>"+
				"- A m�quina esta totalmente abastecida.<br/>"+
				"- Os itens est�o marcados para n�o abastecer.<br/>"+
				"- Os itens est�o em ruptura.<br/>"+
				"- Os itens tem uma quantidade m�nima para abastecimento, e essa quantidade n�o foi atingida. </br><br/>");
				}
			}
		}else {
			if(!validaSeOhPedidoDeAbastecimentoPoderaSerGerado(secosCongelados,linhas.getCampo("CODBEM").toString())) {
				throw new Error("<b> Aten��o </b><br/><br/>"+
			" O pedido de <b>"+tp+"</b> n�o pode ser gerado !<br/>"+
			" causas poss�veis: <br/>"+
			"- A m�quina esta totalmente abastecida.<br/>"+
			"- Os itens est�o marcados para n�o abastecer.<br/>"+
			"- Os itens est�o em ruptura.<br/>"+
			"- Os itens tem uma quantidade m�nima para abastecimento, e essa quantidade n�o foi atingida. </br><br/>");
			}
		}
		
		Timestamp dtAbastecimento = (Timestamp) arg0.getParam("DTABAST");
		Timestamp dtSolicitacao = null;

		if (validaPedido(linhas.getCampo("CODBEM").toString(), secosCongelados)) {
			throw new PersistenceException(
					"<br/>Patrim�nio <b>" + linhas.getCampo("CODBEM") + "</b> j� possui pedido pendente!<br/>");
		}

		if ("1".equals(tipoAbastecimento) && dtAbastecimento != null) {
			dtAbastecimento = null;
		}

		if ("2".equals(tipoAbastecimento) && dtAbastecimento == null) {
			throw new PersistenceException("<b>ERRO!</b> - Pedidos agendados precisam de uma data de abastecimento!");
		}

		if ("2".equals(tipoAbastecimento) && dtAbastecimento != null) {
			dtSolicitacao = TimeUtils.getNow();

			if (dtAbastecimento.before(reduzUmDia(dtSolicitacao))) {
				throw new PersistenceException(
						"<b>ERRO!</b> - Data de agendamento n�o pode ser menor que a data de hoje!");
			}

			int diaDataAgendada = TimeUtils.getDay(dtAbastecimento);
			int diaDataAtual = TimeUtils.getDay(dtSolicitacao);

			int minutoDataAgendada = TimeUtils.getTimeInMinutes(dtAbastecimento);
			int minutoDataAtual = TimeUtils.getTimeInMinutes(dtSolicitacao);

			if (diaDataAtual == diaDataAgendada) {
				if (minutoDataAtual == minutoDataAgendada) {
					dtAbastecimento = null;
				}
			}
		}

		return dtAbastecimento;
	}
	
	private boolean verificarSeAhMaquinaEstaTotalmenteDesabastecida(String patrimonio) {
		boolean valida = false;
		
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
							"SELECT CASE WHEN QTD_VISITAS > 0 AND QTD_VAZIAS = QTD_PLANOGRAMA THEN 'S' ELSE 'N' END AS VALIDACAO "+ 
							"FROM(" + 
							"SELECT "+
							"I.CODBEM, "+
							"(SELECT COUNT(*) FROM GC_PLANOGRAMA WHERE CODBEM=I.CODBEM) AS QTD_PLANOGRAMA, "+
							"(SELECT COUNT(*) FROM GC_PLANOGRAMA WHERE CODBEM=I.CODBEM AND ESTOQUE=0) AS QTD_VAZIAS, "+
							"(SELECT COUNT(*) FROM GC_SOLICITABAST WHERE CODBEM=I.CODBEM AND STATUS='3' AND REABASTECIMENTO='S' AND DTABAST > SYSDATE-15) AS QTD_VISITAS "+
							"FROM GC_INSTALACAO I) WHERE CODBEM='"+patrimonio+"'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				String validacao = contagem.getString("VALIDACAO");
				if ("S".equals(validacao)) {
					valida = true;
				}
			}
			
		} catch (Exception e) {
			salvarException("[verificarSeAhMaquinaEstaTotalmenteDesabastecida] Nao foi possivel verificar se a m�quina esta totalmente desabastecida! patrimonio " + patrimonio 
					+ e.getMessage() + "\n" + e.getCause());
		}
		
		return valida;
	}

	private BigDecimal cadastrarNovoAbastecimento(String patrimonio, String secos, String congelados,
			BigDecimal idflow) {
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
			VO.setProperty("NUMCONTRATO", getContrato(patrimonio));
			VO.setProperty("CODPARC", getParceiro(patrimonio));

			if (rota != 0) {
				VO.setProperty("ROTA", new BigDecimal(rota));
			}

			if (idflow != null) {
				VO.setProperty("IDFLOW", idflow);
			}

			VO.setProperty("SECOS", secos);
			VO.setProperty("CONGELADOS", congelados);

			dwfFacade.createEntity("AD_RETABAST", (EntityVO) VO);

			idAbastecimento = VO.asBigDecimal("ID");

		} catch (Exception e) {
			retornoNegativo = retornoNegativo + e.getMessage();
			salvarException("[cadastrarNovoAbastecimento] Nao foi possivel cadastrar um novo abastecimento! "
					+ e.getMessage() + "\n" + e.getCause());
		}

		return idAbastecimento;
	}

	private void apenasSecos(BigDecimal idAbastecimento, Timestamp dtAbastecimento, String patrimonio)
			throws Exception {
		if (idAbastecimento != null) {
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
				
				//if (!congelado(produto)) {
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
						salvarException(
								"[carregaTeclasNosItensDeAbast] Nao foi possivel salvar as teclas na tela Retornos Abastecimento! "
										+ e.getMessage() + "\n" + e.getCause());
					}
				//}
			}
		}
	}

	private void apenasCongelados(BigDecimal idAbastecimento, Timestamp dtAbastecimento, String patrimonio)
			throws Exception {
		if (idAbastecimento != null) {
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
				
				if (congelado(produto)) {
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
						salvarException(
								"[carregaTeclasNosItensDeAbast] Nao foi possivel salvar as teclas na tela Retornos Abastecimento! "
										+ e.getMessage() + "\n" + e.getCause());
					}
				}
			}
		}
	}

	private boolean congelado(BigDecimal codprod) throws Exception {
		boolean valida = false;

		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?", new Object[] { codprod });
		BigDecimal grupo = VO.asBigDecimal("CODGRUPOPROD");

		DAO = JapeFactory.dao("GrupoProduto");
		DynamicVO VOS = DAO.findOne("CODGRUPOPROD=?", new Object[] { grupo });
		String congelado = VOS.asString("AD_CONGELADOS");

		if ("S".equals(congelado)) {
			valida = true;
		}
		return valida;
	}

	private boolean validaPedido(String patrimonio, String secosCongelados) {
		boolean valida = false;

		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();

			if ("1".equals(secosCongelados)) {
				nativeSql.appendSql("SELECT COUNT(*) FROM GC_SOLICITABAST WHERE CODBEM='" + patrimonio
						+ "' AND STATUS IN ('1','2') AND REABASTECIMENTO='S' AND AD_TIPOPRODUTOS IN ('1') AND NVL(AD_TIPOPRODUTOS,'1')='1'");
			} else if ("2".equals(secosCongelados)) {
				nativeSql.appendSql("SELECT COUNT(*) FROM GC_SOLICITABAST WHERE CODBEM='" + patrimonio
						+ "' AND STATUS IN ('1','2') AND REABASTECIMENTO='S' AND AD_TIPOPRODUTOS IN ('2') AND NVL(AD_TIPOPRODUTOS,'1')='2'");
			} else {
				nativeSql.appendSql("SELECT COUNT(*) FROM GC_SOLICITABAST WHERE CODBEM='" + patrimonio
						+ "' AND STATUS IN ('1','2') AND REABASTECIMENTO='S' AND AD_TIPOPRODUTOS IN ('1','2') AND NVL(AD_TIPOPRODUTOS,'1')='1'");
			}

			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("COUNT(*)");
				if (count >= 1) {
					valida = true;
				}
			}

		} catch (Exception e) {
			salvarException(
					"[validaPedido] Nao foi possivel validar o pedido! " + e.getMessage() + "\n" + e.getCause());
		}

		return valida;
	}

	private DynamicVO agendarAbastecimento(String patrimonio, Timestamp dtSolicitacao, Timestamp dtAgendamento,
			BigDecimal idAbastecimento, String seco, String congelado) {
		
		DynamicVO gc_solicitabast = null;
		
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("GCSolicitacoesAbastecimento");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("CODUSU", getUsuLogado());
			VO.setProperty("STATUS", "1");
			VO.setProperty("DTSOLICIT", dtSolicitacao);
			VO.setProperty("DTAGENDAMENTO", dtAgendamento);
			VO.setProperty("ROTA", new BigDecimal(getRota(patrimonio)));
			VO.setProperty("IDABASTECIMENTO", idAbastecimento);
			VO.setProperty("REABASTECIMENTO", "S");
			VO.setProperty("APENASVISITA", "N");
			VO.setProperty("AD_NUMCONTRATO", getContrato(patrimonio));
			VO.setProperty("AD_CODPARC", getParceiro(patrimonio));

			if ("S".equals(seco)) {
				VO.setProperty("AD_TIPOPRODUTOS", "1");
			}

			if ("S".equals(congelado)) {
				VO.setProperty("AD_TIPOPRODUTOS", "2");
			}

			String body = montarBody(patrimonio);
			if (body != null) {
				VO.setProperty("AD_BODYPLANOGRAMA", body.toCharArray());
			}

			dwfFacade.createEntity("GCSolicitacoesAbastecimento", (EntityVO) VO);

			gc_solicitabast = VO;

		} catch (Exception e) {
			salvarException("[agendarAbastecimento] Nao foi possivel agendar o Abastecimento! patrimonio: " + patrimonio
					+ "\n" + e.getMessage() + "\n" + e.getCause());
		}

		return gc_solicitabast;
	}

	private Timestamp reduzUmDia(Timestamp data) {
		Calendar dataAtual = Calendar.getInstance();
		dataAtual.setTime(data);
		dataAtual.add(Calendar.DAY_OF_MONTH, -1);
		return new Timestamp(dataAtual.getTimeInMillis());
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
			salvarException("[getRota] Nao foi possibel obter a Rota! " + e.getMessage() + "\n" + e.getCause());
		}

		return count;
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

	private BigDecimal getContrato(String patrimonio) throws Exception {
		BigDecimal contrato = null;
		try {
			JapeWrapper DAO = JapeFactory.dao("PATRIMONIO");
			DynamicVO VO = DAO.findOne("CODBEM=?", new Object[] { patrimonio });
			contrato = VO.asBigDecimal("NUMCONTRATO");
		} catch (Exception e) {
			salvarException("[getContrato] nao foi possivel obter o contrato, patrimonio:" + patrimonio + "\n" + e.getMessage()
			+ "\n" + e.getCause());
		}
		
		if(contrato==null) {
			contrato = new BigDecimal(1314);
		}
		
		return contrato;
	}

	private DynamicVO getEnderecamento(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("ENDERECAMENTO");
		DynamicVO VO = DAO.findOne("CODBEM=?", new Object[] { patrimonio });
		return VO;
	}
	
	private DynamicVO getTcscon(BigDecimal contrato) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Contrato");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=?", new Object[] { contrato });
		return VO;
	}
	
	private DynamicVO getAdConfigTelPro(BigDecimal empresa, BigDecimal local) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_CONFIGTELPRO");
		DynamicVO VO = DAO.findOne("CODEMP=? AND CODLOCAL=?", new Object[] { empresa, local });
		return VO;
	}
	
	private DynamicVO getAdPlantas(BigDecimal contrato, BigDecimal id) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("PLANTAS");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=? AND ID=?", new Object[] { contrato, id });
		return VO;
	}
	
	private BigDecimal getParceiro(String patrimonio) throws Exception {
		BigDecimal parceiro = null;
		BigDecimal contrato = null;
		
		try {
			JapeWrapper DAO = JapeFactory.dao("PATRIMONIO");
			DynamicVO VO = DAO.findOne("CODBEM=?", new Object[] { patrimonio });

			if(VO!=null) {
				contrato = VO.asBigDecimal("NUMCONTRATO");
			}
			
			if(contrato!=null) {
				DAO = JapeFactory.dao("Contrato");
				DynamicVO VOS = DAO.findOne("NUMCONTRATO=?", new Object[] { contrato });
				parceiro = VOS.asBigDecimal("CODPARC");
			}
	
		} catch (Exception e) {
			salvarException("[getParceiro] nao foi possivel obter o parceiro:" + patrimonio + "\n" + e.getMessage()
			+ "\n" + e.getCause());
		}
		
		if(parceiro==null) {
			parceiro = new BigDecimal(1);
		}

		return parceiro;
	}

	private DynamicVO getTGFPRO(BigDecimal produto) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?", new Object[] { produto });
		return VO;
	}
	
	private DynamicVO getTGFGRU(BigDecimal grupo) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("GrupoProduto");
		DynamicVO VO = DAO.findOne("CODGRUPOPROD=?", new Object[] { grupo });
		return VO;
	}
	
	private DynamicVO getADPATRIMONIO(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("PATRIMONIO");
		DynamicVO VO = DAO.findOne("CODBEM=?", new Object[] { patrimonio });
		return VO;
	}
	
	/*
	private void chamaPentaho() {

		try {

			String site = (String) MGECoreParameter.getParameter("PENTAHOIP");
			;
			String Key = "Basic Z2FicmllbC5uYXNjaW1lbnRvOkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC_New/Transformation/Sankhya-Pedido/";
			String objName = "J-Loop_visitas_pendentes";

			si.runJob(path, objName);

		} catch (Exception e) {
			salvarException(
					"[chamaPentaho] nao foi possivel chamar o pentaho! " + e.getMessage() + "\n" + e.getCause());
		}
	}
*/
	public String montarBody(Object codbem) {

		int cont = 1;
		String head = "{\"planogram\":{\"items_attributes\": [";
		String bottom = "]}}";

		String body = "";

		try {

			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("teclas", "this.CODBEM = ? ", new Object[] { codbem }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);

				String teclaAlternativa = DynamicVO.asString("TECLAALT");
				String tecla = DynamicVO.asBigDecimal("TECLA").toString();
				String name = "";

				if ("0".equals(tecla)) {
					tecla = String.valueOf(cont);
					cont++;
				}

				if (teclaAlternativa != null) {
					name = teclaAlternativa;
				} else {
					name = tecla;
				}

				BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");

				if (Iterator.hasNext()) {
					body = body + "{" + "\"type\": \"Coil\"," + "\"name\": \"" + name + "\"," + "\"good_id\": "
							+ getGoodId(produto) + "," + "\"capacity\": "
							+ DynamicVO.asBigDecimal("AD_CAPACIDADE").toString() + "," + "\"par_level\": "
							+ DynamicVO.asBigDecimal("AD_NIVELPAR").toString() + "," + "\"alert_level\": "
							+ DynamicVO.asBigDecimal("AD_NIVELALERTA").toString() + "," + "\"desired_price\": "
							+ DynamicVO.asBigDecimal("VLRFUN").add(DynamicVO.asBigDecimal("VLRPAR")).toString() + ","
							+ "\"logical_locator\": " + tecla + "," + "\"status\": \"active\"" + "},";
				} else {
					body = body + "{" + "\"type\": \"Coil\"," + "\"name\": \""
							+ DynamicVO.asBigDecimal("TECLA").toString() + "\"," + "\"good_id\": " + getGoodId(produto)
							+ "," + "\"capacity\": " + DynamicVO.asBigDecimal("AD_CAPACIDADE").toString() + ","
							+ "\"par_level\": " + DynamicVO.asBigDecimal("AD_NIVELPAR").toString() + ","
							+ "\"alert_level\": " + DynamicVO.asBigDecimal("AD_NIVELALERTA").toString() + ","
							+ "\"desired_price\": "
							+ DynamicVO.asBigDecimal("VLRFUN").add(DynamicVO.asBigDecimal("VLRPAR")).toString() + ","
							+ "\"logical_locator\": " + tecla + "," + "\"status\": \"active\"" + "}";
				}

			}

		} catch (Exception e) {
			salvarException("[montarBody] nao foi possivel montar o Body! patrimonio:" + codbem + "\n" + e.getMessage()
					+ "\n" + e.getCause());
		}

		return head + body + bottom;

	}

	public BigDecimal getGoodId(BigDecimal produto) throws Exception {
		BigDecimal id = null;
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?", new Object[] { produto });
		BigDecimal idVerti = VO.asBigDecimal("AD_IDPROVERTI");

		if (idVerti == null) {
			id = new BigDecimal(179707);
		} else {
			id = idVerti;
		}

		return id;
	}

	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "btn_abastecimento");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

		} catch (Exception e) {
			// aqui n�o tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}
}
