package br.com.grancoffee.TelemetriaPropria;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.Timer;

import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.TimeUtils;

import Helpers.WSPentaho;
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
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import br.com.sankhya.ws.ServiceContext;

public class evento_verificaEncerramentoOS implements EventoProgramavelJava {
		
	/**
	 * 17/10/2021 vs 1.4 reformulação do evento, realiza os calculos do encerramento da OS.
	 * 25/10/2021 vs 1.5 Inserido os métodos validaItensRetAbast e validaItensDaAppContagem para garantir que o sistema vá inserir para ajuste apenas os itens corretos. Métodos calculaDadosDaContagem e verificaDadosSemContagem foram comentados, por não serem mais utilizados nessa nova lógica.
	 * 26/10/2021 vs 1.6 Insere nos cálculos a retirada dos retornos que não devem entrar nos calculos. Todos onde o campo REDUZESTOQUE da AD_MOTIVOSRETORNO esteja como "N".
	 * 30/12/2021 vs 1.8 Ajuste do objeto que passa o planograma pendente para o planograma atual
	 * 24/01/2022 vs 1.9 Inserida a validação para não realizar as ações se a OS foi cancelada.
	 * 19/05/2022 vs 2.0 Bloqueado temporáriamente a chamada do objeto pentaho, criado métodos [atualizaNivelPar] e [atualizaPlanAtual] para atualizar o nv. par quando for apenas visita.
	 */
	
	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		
	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		start(arg0);
	}

	private void start(PersistenceEvent arg0) throws Exception {

		DynamicVO VO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		
		BigDecimal numos = VO.asBigDecimal("NUMOS");
		
		String newSituacao = VO.asString("SITUACAO");
		String oldSituacao = oldVO.asString("SITUACAO");
		
		BigDecimal tipo = VO.asBigDecimal("CODCOS");
				
		if("P".equals(oldSituacao) && "F".equals(newSituacao)) {
			
			if(tipo.intValue()!=5) { //executa apenas se for diferente de 5 (cancelado)
				
				if(validaSeEhDaTelemetriaPropria(numos)) {
					atualizaCamposInicial(numos);
					realizaValidacoes(numos);
					atualizaCamposFinal(numos);
					
					/*
					Timer timer = new Timer(1000, new ActionListener() {	
						@Override
						public void actionPerformed(ActionEvent e) {
							chamaPentaho2();				
						}
					});
					timer.setRepeats(false);
					timer.start();
					*/
				}
				
			}	
		}
		
	}
	
	private void atualizaCamposInicial(BigDecimal numos) throws Exception {
		DynamicVO gc_SOLICITABAST = getGC_SOLICITABAST(numos);
		String patrimonio = gc_SOLICITABAST.asString("CODBEM");
		
		// TODO :: Atualiza Saldo de estoque.
		AtualizaSaldoDeEstoque(patrimonio);
	}
	
	private void atualizaCamposFinal(BigDecimal numos) throws Exception {
		DynamicVO gc_SOLICITABAST = getGC_SOLICITABAST(numos);
		atualizaCampoParaFinalizacaoDaVisitaNoPentaho(gc_SOLICITABAST.asString("CODBEM"), numos);
		//TODO :: implementar as rotinas do pentaho via API	
	}
	
	private void realizaValidacoes(BigDecimal numos) {
		try {
			
			DynamicVO gc_SOLICITABAST = getGC_SOLICITABAST(numos);
			
			if(gc_SOLICITABAST!=null) {
				
				String patrimonio = gc_SOLICITABAST.asString("CODBEM");
				BigDecimal idretorno = gc_SOLICITABAST.asBigDecimal("IDABASTECIMENTO");
				
				// TODO :: verificar se houve retorno.
				boolean houveretorno = validaSeHouveRetornos(numos, "QTDRET");
				// TODO :: verificar se houve contagem.
				boolean houvecontagem = validaSeHouveRetornos(numos, "QTDCONTAGEM");
				// TODO :: verificar se houve abastecimento.
				boolean houveabastecimento = validaSeHouveRetornos(numos, "QTDABAST");
				
				identificaPentaho();
				//TODO :: Pegar o saldo do estoque via API
				
				if(houveretorno) {
					atualizaDadosRetorno(idretorno, "S");
				}else {
					atualizaDadosRetorno(idretorno, "N");
				}
				
				if(houvecontagem) {
					atualizaOutrosDadosContagem(patrimonio, idretorno, TimeUtils.getNow(), "S");	
				}else {
					atualizaOutrosDadosContagem(patrimonio, idretorno, null, "N");	
				}
				
				//calculaDadosDaContagem(numos, idretorno);
				//verificaDadosSemContagem(numos, idretorno);
				
				validaItensRetAbast(idretorno, patrimonio,numos,houvecontagem);
				validaItensDaAppContagem(numos, idretorno, patrimonio, houvecontagem);
				
				//TODO :: valida itens AD_TROCADEGRADE onde o status = Retirar
				validaItensTrocaDeGrade(numos,idretorno,patrimonio,houvecontagem);
				
				
				if(houveabastecimento) {
					atualizaDadosAbastecimento(patrimonio);
					//TODO :: planograma pendente para atual
					validaDadosAbastecimento(numos, gc_SOLICITABAST);
				}else {
					atualizaNivelPar(numos, gc_SOLICITABAST);
				}
				
			}
			
		} catch (Exception e) {
			salvarException("[realizaValidacoes] nao foi possivel calcular realizar as validações " + numos
					+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void validaItensRetAbast(BigDecimal idretorno, String patrimonio, BigDecimal numos, boolean houvecontagem) {
		
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_ITENSRETABAST","this.ID = ? ", new Object[] { idretorno }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
			String tecla = DynamicVO.asString("TECLA");
			BigDecimal qtdpedido = null;
			BigDecimal contagem = null;
			BigDecimal saldoapos = null;
			BigDecimal retorno = null;
			BigDecimal diferenca = null;
			BigDecimal retornoParaCalculo = null;
			BigDecimal estoque = BigDecimalUtil.getValueOrZero(getSaldoEstoque(patrimonio,produto,tecla));
			BigDecimal capacidade = BigDecimalUtil.getValueOrZero(DynamicVO.asBigDecimal("CAPACIDADE"));
			BigDecimal nivelpar = BigDecimalUtil.getValueOrZero(DynamicVO.asBigDecimal("NIVELPAR"));
			BigDecimal valor = BigDecimalUtil.getValueOrZero(DynamicVO.asBigDecimal("VALOR"));
			BigDecimal retornosAhSeremIgnorados = getRetornosAhSeremIgnorados(numos,produto,tecla);
			
			DynamicVO linhaDaContagem = getLinhaDaContagem(patrimonio,numos,produto,tecla);
			DynamicVO linhaDaAdTrocaDeGra = getLinhaDaAdTrocaDeGra(patrimonio,numos,produto,tecla);
			
			if(linhaDaContagem!=null) {
				//TODO :: preenche dados com os valores da tela
				qtdpedido = linhaDaContagem.asBigDecimal("QTDABAST");
				contagem = linhaDaContagem.asBigDecimal("QTDCONTAGEM");
			}else {
				
				//TODO :: preenche os dados vazios
				if(linhaDaAdTrocaDeGra!=null) {
					qtdpedido = linhaDaAdTrocaDeGra.asBigDecimal("QTDABAST");
					contagem = linhaDaAdTrocaDeGra.asBigDecimal("QTDCONTAGEM");
				}else {
					qtdpedido = new BigDecimal(0);
					contagem = new BigDecimal(0);
				}	
			
			}
			
			if(linhaDaAdTrocaDeGra!=null) {
				retorno = linhaDaAdTrocaDeGra.asBigDecimal("QTDRET");
			}else {
				retorno = new BigDecimal(0);
			}
			
			BigDecimal saldoesperado = estoque.add(qtdpedido);
			retornoParaCalculo = retorno.subtract(retornosAhSeremIgnorados);
			
			//verifica se houve contagem ou não, pq isso influcencia nos calculos
			
			if(houvecontagem) {
				BigDecimal conteretorno = contagem.add(retornoParaCalculo);
				diferenca = BigDecimalUtil.getValueOrZero(conteretorno.subtract(saldoesperado));
				
				atualizaAD_ITENSRETABAST(idretorno, patrimonio, produto, tecla, estoque, qtdpedido, saldoesperado, contagem, diferenca, contagem, retorno);
				
			}else {
				diferenca = new BigDecimal(0);
				saldoapos = BigDecimalUtil.getValueOrZero(saldoesperado.subtract(retornoParaCalculo));
								
				atualizaAD_ITENSRETABAST(idretorno, patrimonio, produto, tecla, estoque, qtdpedido, saldoesperado,contagem, diferenca, saldoapos, retorno);
			}
			
			insereAD_HISTRETABAST(idretorno,patrimonio,tecla,produto,capacidade,nivelpar,estoque,qtdpedido,saldoesperado,contagem,diferenca,saldoapos,retorno,valor);

			}
		} catch (Exception e) {
			salvarException(
					"[validaItensRetAbast] nao foi possivel verificar os dados do encerramento. numos " + numos
							+ e.getMessage() + "\n" + e.getCause());
		}
		
	}
	
	private BigDecimal getRetornosAhSeremIgnorados(BigDecimal numos, BigDecimal produto, String tecla) {
		BigDecimal qtd = null;
		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT SUM(QTD) AS QTD from AD_APPRETORNO WHERE NUMOS="+numos+" AND CODPROD="+produto+" AND TECLA='"+tecla+"' AND IDRETORNO IN (SELECT ID FROM AD_MOTIVOSRETORNO WHERE REDUZESTOQUE='N')");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				BigDecimal count = contagem.getBigDecimal("QTD");
				if(count!=null) {
					qtd = count;
				}
			}
		} catch (Exception e) {
			salvarException(
					"[getRetornosAhSeremIgnorados] não foi possível verificar a quantidade de retornos a serem ignorados. numos " + numos
							+ e.getMessage() + "\n" + e.getCause());
		}
		
		if(qtd==null) {
			qtd = new BigDecimal(0);
		}
		
		return qtd;
	}
	
	private void validaItensDaAppContagem(BigDecimal numos, BigDecimal idretorno, String patrimonio, boolean houvecontagem) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("AD_APPCONTAGEM", "this.NUMOS = ? ", new Object[] { numos }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);

				BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
				String tecla = DynamicVO.asString("TECLA");
				BigDecimal capacidade = DynamicVO.asBigDecimal("CAPACIDADE");
				BigDecimal nivelpar = DynamicVO.asBigDecimal("NIVELPAR");
				BigDecimal estoque = getSaldoEstoque(patrimonio, produto, tecla);
				BigDecimal qtdpedido = DynamicVO.asBigDecimal("QTDABAST");
				BigDecimal saldoesperado = estoque.add(qtdpedido);
				BigDecimal valor = DynamicVO.asBigDecimal("VALOR");
				BigDecimal contagem = null;
				BigDecimal diferenca = null;
				BigDecimal retorno = null;
				BigDecimal saldoapos = null;

				DynamicVO linhaDaAdTrocaDeGra = getLinhaDaAdTrocaDeGra(patrimonio, numos, produto, tecla);

				if (linhaDaAdTrocaDeGra != null) {
					retorno = linhaDaAdTrocaDeGra.asBigDecimal("QTDRET");
				} else {
					retorno = new BigDecimal(0);
				}

				if (houvecontagem) {
					contagem = DynamicVO.asBigDecimal("QTDCONTAGEM");
					BigDecimal conteretorno = contagem.add(retorno);
					diferenca = conteretorno.subtract(saldoesperado);
					saldoapos = contagem;
				} else {
					contagem = new BigDecimal(0);
					diferenca = new BigDecimal(0);
					saldoapos = saldoesperado.subtract(retorno);
				}

				boolean existe = validaSeExisteAhTeclaNaTelaDeRetorno(idretorno, patrimonio, produto, tecla);

				if (!existe) {
					insereAD_ITENSRETABAST(idretorno, patrimonio, tecla, produto, capacidade, nivelpar, estoque,
							qtdpedido, saldoesperado, contagem, diferenca, saldoapos, retorno, valor);
					
					insereAD_HISTRETABAST(idretorno,patrimonio,tecla,produto,capacidade,nivelpar,estoque,qtdpedido,saldoesperado,contagem,diferenca,saldoapos,retorno,valor);
				}

			}
			
		} catch (Exception e) {
			salvarException(
					"[validaItensDaAppContagem] nao foi possivel validar todos os itens da contagem. numos " + numos
							+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void validaItensTrocaDeGrade(BigDecimal numos, BigDecimal idretorno, String patrimonio, boolean houvecontagem) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("AD_TROCADEGRADE", "this.NUMOS = ? AND this.STATUS_PAR=? ", new Object[] { numos, "RETIRAR" }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);

				BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
				String tecla = DynamicVO.asString("TECLA");
				BigDecimal capacidade = DynamicVO.asBigDecimal("CAPACIDADE");
				BigDecimal nivelpar = DynamicVO.asBigDecimal("NIVELPAR");
				BigDecimal estoque = BigDecimalUtil.getValueOrZero(getSaldoEstoque(patrimonio, produto, tecla));
				BigDecimal qtdpedido = DynamicVO.asBigDecimal("QTDABAST");
				BigDecimal saldoesperado = estoque.add(qtdpedido);
				BigDecimal valor = DynamicVO.asBigDecimal("VALOR");
				BigDecimal contagem = null;
				BigDecimal diferenca = null;
				BigDecimal retorno = DynamicVO.asBigDecimal("QTDRET");
				BigDecimal saldoapos = null;

				if (houvecontagem) {
					contagem = DynamicVO.asBigDecimal("QTDCONTAGEM");
					BigDecimal conteretorno = contagem.add(retorno);
					diferenca = conteretorno.subtract(saldoesperado);
					saldoapos = contagem;
				} else {
					contagem = new BigDecimal(0);
					diferenca = new BigDecimal(0);
					saldoapos = saldoesperado.subtract(retorno);
				}

				boolean existe = validaSeExisteAhTeclaNaTelaDeRetorno(idretorno, patrimonio, produto, tecla);

				if (!existe) {
					insereAD_ITENSRETABAST(idretorno, patrimonio, tecla, produto, capacidade, nivelpar, estoque,
							qtdpedido, saldoesperado, contagem, diferenca, saldoapos, retorno, valor);
					
					insereAD_HISTRETABAST(idretorno,patrimonio,tecla,produto,capacidade,nivelpar,estoque,qtdpedido,saldoesperado,contagem,diferenca,saldoapos,retorno,valor);
				}

			}
			
			
		} catch (Exception e) {
			salvarException(
					"[validaItensTrocaDeGrade] nao foi possivel validar todos os itens da troca de grade. numos " + numos
							+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private DynamicVO getLinhaDaContagem(String patrimonio, BigDecimal numos, BigDecimal produto, String tecla) {
		DynamicVO item = null;
		try {
			
			JapeWrapper DAO = JapeFactory.dao("AD_APPCONTAGEM");
			DynamicVO VO = DAO.findOne("NUMOS=? AND CODPROD=? AND TECLA=?",new Object[] { numos, produto, tecla });

			item = VO;
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return item;
	}
	
	private DynamicVO getLinhaDaAdTrocaDeGra(String patrimonio, BigDecimal numos, BigDecimal produto, String tecla) {
		DynamicVO item = null;
		try {
			
			JapeWrapper DAO = JapeFactory.dao("AD_TROCADEGRADE");
			DynamicVO VO = DAO.findOne("NUMOS=? AND CODPROD=? AND TECLA=? AND CODBEM=?",new Object[] { numos, produto, tecla, patrimonio });

			item = VO;
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return item;
	}
	
	private void atualizaCampoParaFinalizacaoDaVisitaNoPentaho(String patrimonio, BigDecimal numos) {

		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("GCSolicitacoesAbastecimento",
					"this.CODBEM=? AND this.NUMOS=? ", new Object[] { patrimonio, numos }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;
				
				String valida = "S";
				
				VO.setProperty("AD_FINALIZARVISITA", valida);

				itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			salvarException(
					"[atualizaCampoParaFinalizacaoDaVisitaNoPentaho] nao foi possivel atualizar o campo AD_FINALIZARVISITA patrimonio" + patrimonio + " numos "+numos
							+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void validaDadosAbastecimento(BigDecimal numos, DynamicVO gc_SOLICITABAST) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("AD_TROCADEGRADE", "this.NUMOS = ? ", new Object[] { numos }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);

				String patrimonio = DynamicVO.asString("CODBEM");
				String tecla = DynamicVO.asString("TECLA");
				BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
				BigDecimal capacidade = DynamicVO.asBigDecimal("CAPACIDADE");
				BigDecimal nivelpar = DynamicVO.asBigDecimal("NIVELPAR");
				BigDecimal estoque = getSaldoEstoque(patrimonio,produto,tecla);
				String statuspar = DynamicVO.asString("STATUS_PAR");
				BigDecimal vlrpar = DynamicVO.asBigDecimal("VLRPAR");
				BigDecimal vlrfun = DynamicVO.asBigDecimal("VLRFUN");
				BigDecimal nivelalerta = DynamicVO.asBigDecimal("NIVELALERTA");
				
				if("RETIRAR".equals(statuspar)) {
					retirarTecla(patrimonio,tecla,produto);
				}else if("NOVO".equals(statuspar)) {
					insereTecla(capacidade, patrimonio, produto, estoque, gc_SOLICITABAST.asBigDecimal("IDABASTECIMENTO"), gc_SOLICITABAST.asBigDecimal("ID"), nivelalerta,nivelpar,numos,gc_SOLICITABAST.asBigDecimal("NUNOTA"),tecla,vlrfun,vlrpar);
				}else {
					atualizaTecla(capacidade, patrimonio, produto, estoque, gc_SOLICITABAST.asBigDecimal("IDABASTECIMENTO"), gc_SOLICITABAST.asBigDecimal("ID"), nivelalerta,nivelpar,numos,gc_SOLICITABAST.asBigDecimal("NUNOTA"),tecla,vlrfun,vlrpar);
				}
			}
			
		} catch (Exception e) {
			salvarException(
					"[calculaDadosDaContagem] nao foi possivel calcular os dados da contagem. numos " + numos
							+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void atualizaNivelPar(BigDecimal numos, DynamicVO gc_SOLICITABAST) {
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("AD_TROCADEGRADE", "this.NUMOS = ? ", new Object[] { numos }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);

				String patrimonio = DynamicVO.asString("CODBEM");
				String tecla = DynamicVO.asString("TECLA");
				BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
				BigDecimal nivelpar = DynamicVO.asBigDecimal("NIVELPAR");
				
				atualizaPlanAtual(patrimonio,tecla,produto,nivelpar);
				
			}
			
		} catch (Exception e) {
			salvarException(
					"[atualizaNivelPar] nao foi possivel obter os dados. numos " + numos
							+ e.getMessage() + "\n" + e.getCause());
		}
		
	}
	
	private void atualizaPlanAtual(String patrimonio,String tecla,BigDecimal produto,BigDecimal nivelpar) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_PLANOGRAMAATUAL",
					"this.CODBEM=? AND this.TECLA=? AND this.CODPROD=? ", new Object[] { patrimonio, tecla,  produto}));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("NIVELPAR", nivelpar);

				itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			salvarException(
					"[atualizaPlanAtual] nao foi possivel atualizar o plan. atual, patrimônio: " + patrimonio
							+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void atualizaTecla(BigDecimal capacidade, String patrimonio, BigDecimal produto, BigDecimal estoque, BigDecimal idretorno, BigDecimal idsolicitacao, BigDecimal nivelalerta,
			BigDecimal nivelpar, BigDecimal numos, BigDecimal nunota, String tecla, BigDecimal vlrfun, BigDecimal vlrpar) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_PLANOGRAMAATUAL","this.CODBEM=? AND this.CODPROD=? AND this.TECLA=?", new Object[] { patrimonio,produto,tecla }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			DynamicVO VO = (DynamicVO) NVO;

			VO.setProperty("NUMOS", numos);
			VO.setProperty("NUNOTA", nunota);
			VO.setProperty("IDSOLICIT", idsolicitacao);
			VO.setProperty("IDRETORNO", idretorno);
			VO.setProperty("CAPACIDADE", capacidade);
			VO.setProperty("NIVELPAR", nivelpar);
			VO.setProperty("NIVELALERTA", nivelalerta);
			VO.setProperty("ESTOQUE", estoque);
			VO.setProperty("VLRPAR", vlrpar);
			VO.setProperty("VLRFUN", vlrfun);

			itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			salvarException("[atualizaTecla] Nao foi possivel atualizar a tecla! patrimonio: " + patrimonio+ " produto: " + produto + "tecla "+tecla
					+"\n" + e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void insereTecla(BigDecimal capacidade, String patrimonio, BigDecimal produto, BigDecimal estoque, BigDecimal idretorno, BigDecimal idsolicitacao, BigDecimal nivelalerta,
			BigDecimal nivelpar, BigDecimal numos, BigDecimal nunota, String tecla, BigDecimal vlrfun, BigDecimal vlrpar) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_PLANOGRAMAATUAL");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("CAPACIDADE", capacidade);
			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("CODPROD", produto);
			VO.setProperty("ESTOQUE", estoque);
			VO.setProperty("IDRETORNO", idretorno);
			VO.setProperty("IDSOLICIT", idsolicitacao);
			VO.setProperty("NIVELALERTA", nivelalerta);
			VO.setProperty("NIVELPAR", nivelpar);
			VO.setProperty("NUMOS", numos);
			VO.setProperty("NUNOTA", nunota);
			VO.setProperty("TECLA", tecla);
			VO.setProperty("VLRFUN", vlrfun);
			VO.setProperty("VLRPAR", vlrpar);

			dwfFacade.createEntity("AD_PLANOGRAMAATUAL", (EntityVO) VO);
		} catch (Exception e) {
			salvarException("[insereTecla] Nao foi possivel inserir a tecla! patrimonio: " + patrimonio+ " produto: " + produto + "tecla "+tecla
					+"\n" + e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void retirarTecla(String patrimonio, String tecla, BigDecimal produto) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_PLANOGRAMAATUAL", "this.CODBEM=? AND this.TECLA=? AND this.CODPROD=?",new Object[] {patrimonio,tecla,produto}));
		} catch (Exception e) {
			salvarException("[alterarRetorno] Nao foi possivel excluir a tecla! patrimonio: " + patrimonio+ " produto: " + produto + "tecla "+tecla
					+"\n" + e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void atualizaDadosAbastecimento(String patrimonio) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			PersistentLocalEntity PersistentLocalEntity = dwfFacade.findEntityByPrimaryKey("GCInstalacao", patrimonio);
			EntityVO NVO = PersistentLocalEntity.getValueObject();
			DynamicVO appVO = (DynamicVO) NVO;
							 
			appVO.setProperty("AD_RETIRARPECAS", "N");
							 
			PersistentLocalEntity.setValueObject(NVO);
		} catch (Exception e) {
			salvarException(
					"[atualizaDadosAbastecimento] nao foi possivel atualizar o campo AD_RETIRARPECAS" + patrimonio
							+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void atualizaDadosRetorno(BigDecimal idretorno, String retorno) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			PersistentLocalEntity PersistentLocalEntity = dwfFacade.findEntityByPrimaryKey("AD_RETABAST", idretorno);
			EntityVO NVO = PersistentLocalEntity.getValueObject();
			DynamicVO appVO = (DynamicVO) NVO;
							 
			appVO.setProperty("RETORNO", retorno);
							 
			PersistentLocalEntity.setValueObject(NVO);
		} catch (Exception e) {
			salvarException(
					"[atualizaDadosRetorno] nao foi possivel atualizar dados do retorno id retorno " + idretorno
							+ e.getMessage() + "\n" + e.getCause());
		}
	}

	private void atualizaOutrosDadosContagem(String patrimonio, BigDecimal idretorno, Timestamp ultimacontagem, String contagem) {
		
		if(ultimacontagem!=null) {
			//atualiza data última contagem
			try {
				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				PersistentLocalEntity PersistentLocalEntity = dwfFacade.findEntityByPrimaryKey("GCInstalacao", patrimonio);
				EntityVO NVO = PersistentLocalEntity.getValueObject();
				DynamicVO appVO = (DynamicVO) NVO;
								 
				appVO.setProperty("AD_DTULTCONTAGEM", TimeUtils.getNow());
								 
				PersistentLocalEntity.setValueObject(NVO);
			} catch (Exception e) {
				salvarException(
						"[atualizaOutrosDados] nao foi possivel atualizar data da ultima contagem " + patrimonio
								+ e.getMessage() + "\n" + e.getCause());
			}
		}

		//atualiza houve contagem
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			PersistentLocalEntity PersistentLocalEntity = dwfFacade.findEntityByPrimaryKey("AD_RETABAST", idretorno);
			EntityVO NVO = PersistentLocalEntity.getValueObject();
			DynamicVO appVO = (DynamicVO) NVO;
							 
			appVO.setProperty("CONTAGEM", contagem);
							 
			PersistentLocalEntity.setValueObject(NVO);
		} catch (Exception e) {
			salvarException(
					"[atualizaOutrosDados] nao foi possivel atualizar data da ultima contagem " + patrimonio
							+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void AtualizaSaldoDeEstoque(String patrimonio) {
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("GCInstalacao",
					"this.CODBEM=?", new Object[] { patrimonio}));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;
				
				String atualiza = "S";
				
				VO.setProperty("AD_ATUALIZASALDO", atualiza);

				itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			salvarException(
					"[AtualizaSaldoDeEstoque] nao foi possível atualizar o saldo de estoque patrimonio " + patrimonio
							+ e.getMessage() + "\n" + e.getCause());
		}
		
	}
	
	private void insereAD_HISTRETABAST(BigDecimal idretorno, String patrimonio, String tecla, BigDecimal produto, BigDecimal capacidade, BigDecimal nivelpar,
			BigDecimal saldoanterior, BigDecimal qtdpedido, BigDecimal saldoesperado, BigDecimal contagem, BigDecimal diferenca, BigDecimal saldoapos, BigDecimal qtdretorno, BigDecimal valor) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_HISTRETABAST");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("ID", idretorno);
			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("TECLA", tecla);
			VO.setProperty("CODPROD", produto);
			VO.setProperty("CAPACIDADE", capacidade);
			VO.setProperty("NIVELPAR", nivelpar);
			VO.setProperty("SALDOANTERIOR", saldoanterior);
			VO.setProperty("QTDPEDIDO", qtdpedido);
			VO.setProperty("SALDOESPERADO", saldoesperado);
			VO.setProperty("CONTAGEM", contagem);
			VO.setProperty("DIFERENCA", diferenca);
			VO.setProperty("SALDOAPOS", saldoapos);
			VO.setProperty("QTDRETORNO", qtdretorno);
			VO.setProperty("VALOR", valor);

			dwfFacade.createEntity("AD_HISTRETABAST", (EntityVO) VO);
		} catch (Exception e) {
			salvarException(
					"[insereAD_HISTRETABAST] nao foi possivel inserir a tela de retorno no histórico "+patrimonio+" retorno "+idretorno+ " produto "+produto
							+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void insereAD_ITENSRETABAST(BigDecimal idretorno, String patrimonio, String tecla, BigDecimal produto, BigDecimal capacidade, BigDecimal nivelpar,
			BigDecimal saldoanterior, BigDecimal qtdpedido, BigDecimal saldoesperado, BigDecimal contagem, BigDecimal diferenca, BigDecimal saldoapos, BigDecimal qtdretorno, BigDecimal valor) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_ITENSRETABAST");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("ID", idretorno);
			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("TECLA", tecla);
			VO.setProperty("CODPROD", produto);
			VO.setProperty("CAPACIDADE", capacidade);
			VO.setProperty("NIVELPAR", nivelpar);
			VO.setProperty("SALDOANTERIOR", saldoanterior);
			VO.setProperty("QTDPEDIDO", qtdpedido);
			VO.setProperty("SALDOESPERADO", saldoesperado);
			VO.setProperty("CONTAGEM", contagem);
			VO.setProperty("DIFERENCA", diferenca);
			VO.setProperty("SALDOAPOS", saldoapos);
			VO.setProperty("QTDRETORNO", qtdretorno);
			VO.setProperty("VALOR", valor);

			dwfFacade.createEntity("AD_ITENSRETABAST", (EntityVO) VO);
		} catch (Exception e) {
			salvarException(
					"[insereAD_ITENSRETABAST] nao foi possivel inserir a tela de retorno "+patrimonio+" retorno "+idretorno+ " produto "+produto
							+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private boolean validaSeExisteAhTeclaNaTelaDeRetorno(BigDecimal idretorno, String patrimonio, BigDecimal produto, String tecla) {
		boolean valida = false;

		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT COUNT(*) FROM AD_ITENSRETABAST WHERE ID="+idretorno+" AND CODBEM='"+patrimonio+"' AND CODPROD="+produto+" AND TECLA='"+tecla+"'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("COUNT(*)");
				if (count >= 1) {
					valida = true;
				}
			}

		} catch (Exception e) {
			salvarException(
					"[validaSeExisteAhTeclaNaTelaDeRetorno] nao foi possivel validar se a tecla existe na tela de retorno "+patrimonio+" retorno "+idretorno+ " produto "+produto
							+ e.getMessage() + "\n" + e.getCause());
		}

		return valida;
	}
	
	private void atualizaAD_ITENSRETABAST(BigDecimal idretorno, String patrimonio, BigDecimal produto, String tecla,
			BigDecimal saldoanterior, BigDecimal qtdpedido, BigDecimal saldoesperado, BigDecimal contagem, BigDecimal diferenca, BigDecimal saldoapos, BigDecimal qtdretorno) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_ITENSRETABAST",
					"this.ID=? AND this.CODBEM=? AND this.CODPROD=? AND this.TECLA=? ", new Object[] { idretorno, patrimonio,produto,tecla }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("SALDOANTERIOR", saldoanterior);
				VO.setProperty("QTDPEDIDO", qtdpedido);
				VO.setProperty("SALDOESPERADO", saldoesperado);
				VO.setProperty("CONTAGEM", contagem);
				VO.setProperty("DIFERENCA", diferenca);
				VO.setProperty("SALDOAPOS", saldoapos);
				VO.setProperty("QTDRETORNO", qtdretorno);

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			salvarException(
					"[atualizaAD_ITENSRETABAST] nao foi possivel atualizar a tela de retorno "+patrimonio+" retorno "+idretorno+ " produto "+produto
							+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private BigDecimal getSaldoEstoque(String patrimonio, BigDecimal produto, String tecla) throws Exception {
		BigDecimal estoque = null;
		JapeWrapper DAO = JapeFactory.dao("AD_ESTOQUE");
		DynamicVO VO = DAO.findOne("CODBEM=? AND TECLA=? AND CODPROD=?",new Object[] { patrimonio, tecla, produto});
		if(VO!=null) {
			estoque = VO.asBigDecimal("ESTOQUE");
		}
		
		if(estoque==null) {
			estoque = new BigDecimal(0);
		}
		
		return estoque;
	}
	
	private DynamicVO getGC_SOLICITABAST(BigDecimal numos) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("GCSolicitacoesAbastecimento");
		DynamicVO VO = DAO.findOne("NUMOS=?",new Object[] { numos });
		return VO;
	}
	
	private void identificaPentaho() {
		Timer timer = new Timer(1000, new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				chamaPentaho();				
			}
		});
		timer.setRepeats(false);
		timer.start();
	}
	
	private void chamaPentaho() {

		try {

			String site = (String) MGECoreParameter.getParameter("PENTAHOIP");;
			String Key = "Basic Z2FicmllbC5uYXNjaW1lbnRvOkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC_New/Transformation/Sankhya-Atualiza_saldo_estoque/";
			String objName = "T-TF_Atualiza_saldo";

			si.runTrans(path, objName);

		} catch (Exception e) {
			salvarException("[chamaPentaho] nao foi possivel chamar o pentaho! "+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private boolean validaSeHouveRetornos(BigDecimal numos, String campo) {
		boolean valida = false;

		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT COUNT(*) FROM AD_TROCADEGRADE WHERE "+campo+">0 AND NUMOS="+numos);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("COUNT(*)");
				if (count >= 1) {
					valida = true;
				}
			}

		} catch (Exception e) {
			salvarException(
					"[validaSeEhDaTelemetriaPropria] nao foi possivel verificar se e uma OS da telemetria propria. "
							+ e.getMessage() + "\n" + e.getCause());
		}

		return valida;
	}
	
	private void chamaPentaho2() {

		try {

			String site = (String) MGECoreParameter.getParameter("PENTAHOIP");
			String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC_New/Transformation/Sankhya-EncerramentoOS/";
			String objName = "J-Loop_visitas_encerradas";

			si.runJob(path, objName);

		} catch (Exception e) {
			e.getMessage();
		}
	}
	
	private boolean validaSeEhDaTelemetriaPropria(BigDecimal numos) {
		boolean valida = false;
		
		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("select COUNT(*) from gc_solicitabast where numos=" + numos);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("COUNT(*)");
				if (count >= 1) {
					valida = true;
				}
			}

		} catch (Exception e) {
			salvarException(
					"[validaSeEhDaTelemetriaPropria] nao foi possivel verificar se e uma OS da telemetria propria. "
							+ e.getMessage() + "\n" + e.getCause());
		}

		return valida;
	}
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "evento_verificaEncerramentoOS");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

		} catch (Exception e) {
			// aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}
	
	/*
	 * private void calculaDadosDaContagem(BigDecimal numos, BigDecimal idretorno) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("AD_TROCADEGRADE", "this.NUMOS = ? ", new Object[] { numos }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);

				BigDecimal retorno = null;
				String patrimonio = DynamicVO.asString("CODBEM");
				String tecla = DynamicVO.asString("TECLA");
				BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
				BigDecimal capacidade = DynamicVO.asBigDecimal("CAPACIDADE");
				BigDecimal nivelpar = DynamicVO.asBigDecimal("NIVELPAR");
				BigDecimal qtdpedido = DynamicVO.asBigDecimal("QTDABAST");
				BigDecimal contagem = DynamicVO.asBigDecimal("QTDCONTAGEM");
				BigDecimal ret = DynamicVO.asBigDecimal("QTDRET");
				
				if(ret!=null) {
					retorno = ret;
				}else {
					retorno = new BigDecimal(0);
				}
				
				BigDecimal valor = DynamicVO.asBigDecimal("VALOR");
				BigDecimal estoque = getSaldoEstoque(patrimonio,produto,tecla);
				BigDecimal saldoEsperado = estoque.add(qtdpedido);
				BigDecimal conteretorno = contagem.add(retorno);
				BigDecimal diferenca = conteretorno.subtract(saldoEsperado);
				
				boolean existe = validaSeExisteAhTeclaNaTelaDeRetorno(idretorno, patrimonio, produto, tecla);
				
				if(existe) {
					atualizaAD_ITENSRETABAST(idretorno, patrimonio, produto, tecla, estoque, qtdpedido, saldoEsperado, contagem, diferenca, contagem, retorno);
				}else {
					insereAD_ITENSRETABAST(idretorno,patrimonio,tecla,produto,capacidade,nivelpar,estoque,qtdpedido,saldoEsperado,contagem,diferenca,contagem,retorno,valor);
				}
				
				boolean existeHistorico = validaSeExisteNaTelaDeHistorico(idretorno, patrimonio, produto, tecla);
				
				if(existeHistorico) {
					atualizaAD_HISTRETABAST(idretorno, patrimonio, produto, tecla, estoque, qtdpedido, saldoEsperado, contagem, diferenca, contagem, retorno);
				}else {
					insereAD_HISTRETABAST(idretorno,patrimonio,tecla,produto,capacidade,nivelpar,estoque,qtdpedido,saldoEsperado,contagem,diferenca,contagem,retorno,valor);
				}
				
				
			}
			
		} catch (Exception e) {
			salvarException(
					"[calculaDadosDaContagem] nao foi possivel calcular os dados da contagem. numos " + numos
							+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void atualizaAD_HISTRETABAST(BigDecimal idretorno, String patrimonio, BigDecimal produto, String tecla,
			BigDecimal saldoanterior, BigDecimal qtdpedido, BigDecimal saldoesperado, BigDecimal contagem, BigDecimal diferenca, BigDecimal saldoapos, BigDecimal qtdretorno) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_HISTRETABAST",
					"this.ID=? AND this.CODBEM=? AND this.CODPROD=? AND this.TECLA=? ", new Object[] { idretorno, patrimonio,produto,tecla }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("SALDOANTERIOR", saldoanterior);
				VO.setProperty("QTDPEDIDO", qtdpedido);
				VO.setProperty("SALDOESPERADO", saldoesperado);
				VO.setProperty("CONTAGEM", contagem);
				VO.setProperty("DIFERENCA", diferenca);
				VO.setProperty("SALDOAPOS", saldoapos);
				VO.setProperty("QTDRETORNO", qtdretorno);

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			salvarException(
					"[atualizaAD_ITENSRETABAST] nao foi possivel atualizar a tela de retorno "+patrimonio+" retorno "+idretorno+ " produto "+produto
							+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private boolean validaSeExisteNaTelaDeHistorico(BigDecimal idretorno, String patrimonio, BigDecimal produto, String tecla) {
		boolean valida = false;

		try {
			
			JapeWrapper DAO = JapeFactory.dao("AD_HISTRETABAST");
			DynamicVO VO = DAO.findOne("ID=? AND CODBEM=? AND CODPROD=? AND TECLA=?",new Object[] { idretorno, patrimonio, produto, tecla });
			
			if(VO!=null) {
				valida=true;
			}


		} catch (Exception e) {
			salvarException(
					"[validaSeExisteAhTeclaNaTelaDeRetorno] nao foi possivel validar se a tecla existe na tela de retorno "+patrimonio+" retorno "+idretorno+ " produto "+produto
							+ e.getMessage() + "\n" + e.getCause());
		}

		return valida;
	}
	
	private void verificaDadosSemContagem(BigDecimal numos, BigDecimal idretorno) {
		try {

			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
					new FinderWrapper("AD_TROCADEGRADE", "this.NUMOS = ? ", new Object[] { numos }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);
				
				BigDecimal retorno = null;
				String patrimonio = DynamicVO.asString("CODBEM");
				String tecla = DynamicVO.asString("TECLA");
				BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
				BigDecimal capacidade = DynamicVO.asBigDecimal("CAPACIDADE");
				BigDecimal nivelpar = DynamicVO.asBigDecimal("NIVELPAR");
				BigDecimal qtdpedido = DynamicVO.asBigDecimal("QTDABAST");
				BigDecimal contagem = DynamicVO.asBigDecimal("QTDCONTAGEM");
				BigDecimal ret = DynamicVO.asBigDecimal("QTDRET");
				
				if(ret!=null) {
					retorno = ret;
				}else {
					retorno = new BigDecimal(0);
				}
				
				BigDecimal valor = DynamicVO.asBigDecimal("VALOR");
				BigDecimal estoque = getSaldoEstoque(patrimonio, produto, tecla);
				BigDecimal saldoEsperado = estoque.add(qtdpedido);
				BigDecimal diferenca = new BigDecimal(0);
				BigDecimal saldoapos = saldoEsperado.subtract(retorno);

				boolean existe = validaSeExisteAhTeclaNaTelaDeRetorno(idretorno, patrimonio, produto, tecla);

				if (existe) {
					atualizaAD_ITENSRETABAST(idretorno, patrimonio, produto, tecla, estoque, qtdpedido, saldoEsperado,
							contagem, diferenca, saldoapos, retorno);
				} else {
					insereAD_ITENSRETABAST(idretorno, patrimonio, tecla, produto, capacidade, nivelpar, estoque,
							qtdpedido, saldoEsperado, contagem, diferenca, saldoapos, retorno, valor);
				}

				boolean existeHistorico = validaSeExisteNaTelaDeHistorico(idretorno, patrimonio, produto, tecla);
				
				if(existeHistorico) {
					atualizaAD_HISTRETABAST(idretorno, patrimonio, produto, tecla, estoque, qtdpedido, saldoEsperado, contagem, diferenca, contagem, retorno);
				}else {
					insereAD_HISTRETABAST(idretorno,patrimonio,tecla,produto,capacidade,nivelpar,estoque,qtdpedido,saldoEsperado,contagem,diferenca,contagem,retorno,valor);
				}
				
				
			}

		} catch (Exception e) {
			salvarException("[calculaDadosDaContagem] nao foi possivel calcular os dados da contagem. numos " + numos
					+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	
	 */
}
