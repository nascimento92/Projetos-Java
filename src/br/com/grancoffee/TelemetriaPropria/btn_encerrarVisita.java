package br.com.grancoffee.TelemetriaPropria;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.Timer;

import com.sankhya.util.TimeUtils;

import Helpers.WSPentaho;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
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
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import br.com.sankhya.ws.ServiceContext;

public class btn_encerrarVisita implements AcaoRotinaJava{

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Integer n = (Integer) arg0.getParam("NUMOS");
		
		BigDecimal numos = new BigDecimal(n);
		
		atualizaCamposInicial(numos);
		realizaValidacoes(numos);
		atualizaCamposFinal(numos);
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
				
				// TODO :: verificar se houve retorno.
				boolean houveretorno = validaSeHouveRetornos(numos, "QTDRET");
				// TODO :: verificar se houve contagem.
				boolean houvecontagem = validaSeHouveRetornos(numos, "QTDCONTAGEM");
				// TODO :: verificar se houve abastecimento.
				boolean houveabastecimento = validaSeHouveRetornos(numos, "QTDABAST");
				
				identificaPentaho();
				//TODO :: Pegar o saldo do estoque via API
				
				if(houveretorno) {
					atualizaDadosRetorno(gc_SOLICITABAST.asBigDecimal("IDABASTECIMENTO"), "S");
				}else {
					atualizaDadosRetorno(gc_SOLICITABAST.asBigDecimal("IDABASTECIMENTO"), "N");
				}
				
				if(houvecontagem) {
					
					atualizaOutrosDadosContagem(gc_SOLICITABAST.asString("CODBEM"), gc_SOLICITABAST.asBigDecimal("IDABASTECIMENTO"), TimeUtils.getNow(), "S");
					calculaDadosDaContagem(numos, gc_SOLICITABAST.asBigDecimal("IDABASTECIMENTO"));
					
				}else {
					
					atualizaOutrosDadosContagem(gc_SOLICITABAST.asString("CODBEM"), gc_SOLICITABAST.asBigDecimal("IDABASTECIMENTO"), null, "N");
					verificaDadosSemContagem(numos, gc_SOLICITABAST.asBigDecimal("IDABASTECIMENTO"));
					
				}
				
				
				if(houveabastecimento) {
					atualizaDadosAbastecimento(gc_SOLICITABAST.asString("CODBEM"));
					//TODO :: planograma pendente para atual
					validaDadosAbastecimento(numos, gc_SOLICITABAST);
				}
				
			}
			
		} catch (Exception e) {
			salvarException("[realizaValidacoes] nao foi possivel calcular realizar as validações " + numos
					+ e.getMessage() + "\n" + e.getCause());
		}
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
				}
				
				if("NOVO".equals(statuspar)) {
					insereTecla(capacidade, patrimonio, produto, estoque, gc_SOLICITABAST.asBigDecimal("IDABASTECIMENTO"), gc_SOLICITABAST.asBigDecimal("ID"), nivelalerta,nivelpar,numos,gc_SOLICITABAST.asBigDecimal("NUNOTA"),tecla,vlrfun,vlrpar);
				}
				
				if("IGUAL".equals(statuspar)) {
					atualizaTecla(capacidade, patrimonio, produto, estoque, gc_SOLICITABAST.asBigDecimal("IDABASTECIMENTO"), gc_SOLICITABAST.asBigDecimal("ID"), nivelalerta,nivelpar,numos,gc_SOLICITABAST.asBigDecimal("NUNOTA"),tecla,vlrfun,vlrpar);
				}
				
			}
			
		} catch (Exception e) {
			salvarException(
					"[calculaDadosDaContagem] nao foi possivel calcular os dados da contagem. numos " + numos
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
	
	
	private void calculaDadosDaContagem(BigDecimal numos, BigDecimal idretorno) {
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

			dwfFacade.createEntity("AD_ITENSRETABAST", (EntityVO) VO);
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
	
	
}