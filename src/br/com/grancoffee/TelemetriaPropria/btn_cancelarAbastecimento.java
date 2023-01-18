package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import com.sankhya.util.TimeUtils;
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
import br.com.sankhya.modelcore.comercial.ComercialUtils;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btn_cancelarAbastecimento implements AcaoRotinaJava {
	int cont=0;
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {

		if (arg0.getLinhas().length > 1) {
			arg0.mostraErro("<b>Selecione apenas uma linha!</b>");
		} else {
			Registro[] linhas = arg0.getLinhas();
			BigDecimal nunota = (BigDecimal) linhas[0].getCampo("NUNOTA");
			BigDecimal numos = (BigDecimal) linhas[0].getCampo("NUMOS");
			String status = (String) linhas[0].getCampo("STATUS");
			String motivo = (String) arg0.getParam("MOTIVO");
			BigDecimal idretorno = (BigDecimal) linhas[0].getCampo("IDABASTECIMENTO");
			
			boolean confirmarSimNao = false;

			if ("1".equals(status)) {
				if (nunota == null && numos == null) {
					confirmarSimNao = arg0.confirmarSimNao("Atenção", "A solicitação ainda não possui um pedido nem uma OS, mesmo assim deseja cancelar?", 1);
				}else {
					
					if (nunota != null && numos != null) {
						DynamicVO tgfVar = getTgfVar(nunota);
						if(tgfVar!=null) {
							confirmarSimNao = arg0.confirmarSimNao("Atenção", "Pedido <b>"+nunota+"</b> já faturada, deseja cancelar apenas a OS ? O sistema automáticamente criará uma nota de devolução, continuar ?", 1);
						}else {
							confirmarSimNao = arg0.confirmarSimNao("Atenção", "O pedido <b>"+nunota+"</b> será excluido do portal de vendas, e a OS <b>"+numos+"</b> será cancelada, continuar?", 1);
						}
						
					}else if(nunota != null && numos == null) {
						DynamicVO tgfVar = getTgfVar(nunota);
						if(tgfVar!=null) {
							confirmarSimNao = arg0.confirmarSimNao("Atenção", "O pedido <b>"+nunota+"</b> já foi faturado, será gerada uma nota de devolução, continuar?", 1);	
						}else {
							confirmarSimNao = arg0.confirmarSimNao("Atenção", "O pedido <b>"+nunota+"</b> jserá excluido, continuar?", 1);
						}
						
					}else if(nunota == null && numos != null) {
						confirmarSimNao = arg0.confirmarSimNao("Atenção", "A OS <b>"+numos+"</b> será cancelada, continuar?", 1);	
					}
				}
				
				if(confirmarSimNao) {
					if (nunota != null && numos != null) {
						
						DynamicVO tgfVar = getTgfVar(nunota);
						if (tgfVar != null) {	
							
							BigDecimal nunotaTopDestino = tgfVar.asBigDecimal("NUNOTA");
							BigDecimal nunotaDev = geraNotaDevolucao(nunotaTopDestino);
							if (nunotaDev != null) {
								listaItensNotaModelo(nunotaTopDestino,nunotaDev);
								linhas[0].setCampo("AD_NUNOTADEV", nunotaDev);
								totalizaImpostos(nunotaDev);
							}
							
							
							DynamicVO tabelaTcsite = getTcsite(numos);
							BigDecimal codusurel = tabelaTcsite.asBigDecimal("CODUSU");
							insertTcsrus(numos,codusurel);
							cancelarSubOS(numos);
							cancelarOS(numos);		
							
						}else {
							
							if(validaSeOhPedidoEstaEmOrdemDeCarga(nunota)) {
								throw new Error("<br/><b>ATENÇÃO</b><br/><br/>O pedido <b>"+nunota+"</b> está em ordem de carga, não é possível prosseguir com o cancelamento da visita, retire o pedido da O.C para prosseguir! <br/><br/>");
							}else {
								DynamicVO tabelaTcsite = getTcsite(numos);
								BigDecimal codusurel = tabelaTcsite.asBigDecimal("CODUSU");
								insertTcsrus(numos,codusurel);
								excluirNota(nunota);
								cancelarSubOS(numos);
								cancelarOS(numos);
							}
	
						}
						
					}else
						if(nunota != null && numos == null) {
							
							DynamicVO tgfVar = getTgfVar(nunota);
							if (tgfVar != null) {
								
								System.out.println(" ");
								
								BigDecimal nunotaTopDestino = tgfVar.asBigDecimal("NUNOTA");
								BigDecimal nunotaDev = geraNotaDevolucao(nunotaTopDestino);
								if (nunotaDev != null) {
									listaItensNotaModelo(nunotaTopDestino,nunotaDev);
									linhas[0].setCampo("AD_NUNOTADEV", nunotaDev);
									totalizaImpostos(nunotaDev);
								}
								
							}else {
								if(validaSeOhPedidoEstaEmOrdemDeCarga(nunota)) {
									throw new Error("<br/><b>ATENÇÃO</b><br/><br/>O pedido <b>"+nunota+"</b> está em ordem de carga, não é possível prosseguir com o cancelamento da visita, retire o pedido da O.C para prosseguir! <br/><br/>");
								}else {
									excluirNota(nunota);
								}
							}
							
						}
						else
							if(nunota == null && numos != null) {
								DynamicVO tabelaTcsite = getTcsite(numos);
								BigDecimal codusurel = tabelaTcsite.asBigDecimal("CODUSU");
								insertTcsrus(numos,codusurel);
								cancelarSubOS(numos);
								cancelarOS(numos);		
							}
					this.cont++;
					excluirRetornoAbastecimento(idretorno);
					linhas[0].setCampo("STATUS", "4");
			        linhas[0].setCampo("AD_DTCANCELAMENTO", TimeUtils.getNow());
			        linhas[0].setCampo("AD_CODUSUCANCEL", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
			        linhas[0].setCampo("AD_MOTCANCEL", motivo);
				}
			}else if("4".equals(status)) {
					arg0.mostraErro("<b>Abastecimento já se encontra cancelado!</b>");
				}else {
					arg0.mostraErro("<b>Abastecimento já se encontra finalizado! não é possível cancela-lo!</b>");
				}
		}
		if(cont>0) {
			arg0.setMensagemRetorno("Visita Cancelada!");
		}else {
			arg0.setMensagemRetorno("Ops, algo deu errado!");
		}
		
	}
	
	private BigDecimal geraNotaDevolucao(BigDecimal nunotaFaturada) {
		
		  BigDecimal nunota = null;
		  
		  BigDecimal nuNotaModelo = nunotaFaturada;
		  
		  try { 
			 
			  EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade(); 
			  EntityVO padraoNPVO = dwfFacade.getDefaultValueObjectInstance(DynamicEntityNames.CABECALHO_NOTA);
			  DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("CabecalhoNota", nuNotaModelo);
			  DynamicVO NotaProdVO = (DynamicVO) padraoNPVO;
		  
			  DynamicVO topRVO = ComercialUtils.getTipoOperacao(new BigDecimal(1854)); 
			  String tipoMovimento = topRVO.asString("TIPMOV");
			  
			  BigDecimal empresa = ModeloNPVO.asBigDecimal("CODEMP");
			  DynamicVO tsiemp = getTSIEMP(empresa);
			  BigDecimal codparc = null;
			  if(tsiemp!=null) {
				  BigDecimal codparcEmpresa = tsiemp.asBigDecimal("CODPARC");
				  if(codparcEmpresa!=null) {
					  codparc = codparcEmpresa; 
				  }else {
					  codparc = new BigDecimal(2);
				  }
			  }else {
				  codparc = new BigDecimal(2);
			  }
			  
			  NotaProdVO.setProperty("CODEMP", empresa);
			  NotaProdVO.setProperty("CODTIPOPER", topRVO.asBigDecimal("CODTIPOPER"));
			  NotaProdVO.setProperty("TIPMOV", tipoMovimento);
			  NotaProdVO.setProperty("SERIENOTA", ModeloNPVO.asString("SERIENOTA"));
			  NotaProdVO.setProperty("CODPARC", codparc);
			  NotaProdVO.setProperty("NUMCONTRATO", ModeloNPVO.asBigDecimal("NUMCONTRATO"));
			  NotaProdVO.setProperty("CODTIPVENDA", ModeloNPVO.asBigDecimal("CODTIPVENDA")); 
			  NotaProdVO.setProperty("CODNAT", ModeloNPVO.asBigDecimal("CODNAT"));
			  NotaProdVO.setProperty("CODCENCUS", ModeloNPVO.asBigDecimal("CODCENCUS"));
			  NotaProdVO.setProperty("NUMNOTA", new java.math.BigDecimal(0));
			  NotaProdVO.setProperty("APROVADO", ModeloNPVO.asString("APROVADO"));
			  NotaProdVO.setProperty("PENDENTE", "S"); 
			  NotaProdVO.setProperty("CIF_FOB", ModeloNPVO.asString("CIF_FOB")); 
			  NotaProdVO.setProperty("DTNEG", TimeUtils.getNow());
			  NotaProdVO.setProperty("AD_CODLOCAL", ModeloNPVO.asBigDecimal("AD_CODLOCAL"));
			  NotaProdVO.setProperty("AD_CODBEM", ModeloNPVO.asString("AD_CODBEM"));
			  NotaProdVO.setProperty("OBSERVACAO", "Devolução da nota NU: "+nunotaFaturada);
			  NotaProdVO.setProperty("CODVEND", ModeloNPVO.asBigDecimal("CODVEND"));
			  NotaProdVO.setProperty("CODUSUINC", new BigDecimal(3082));
			  NotaProdVO.setProperty("CODEMPNEGOC", empresa); 
			  NotaProdVO.setProperty("TIPFRETE", "N");
	
			  dwfFacade.createEntity(DynamicEntityNames.CABECALHO_NOTA, (EntityVO) NotaProdVO); 
			  nunota = NotaProdVO.asBigDecimal("NUNOTA");
	
		  } catch (Exception e) {
		  salvarException("[geraCabecalho] Nao foi possivel gerar cabecalho!"+e.getMessage()+"\n"+e.getCause()); 
		  } 
		  
		  return nunota;
	}
	
	
	private void listaItensNotaModelo(BigDecimal nunotaModelo, BigDecimal nunotaDevolucao) {
		
		try {
			DynamicVO tgfcabModelo = getTGFCAB(nunotaModelo);
			BigDecimal localDestino = tgfcabModelo.asBigDecimal("AD_CODLOCAL");
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("ItemNota","this.NUNOTA = ? AND this.SEQUENCIA > 0", new Object[] { nunotaModelo }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			
			BigDecimal codprod = DynamicVO.asBigDecimal("CODPROD");
			BigDecimal localorig = DynamicVO.asBigDecimal("CODLOCALORIG");
			String codvol = DynamicVO.asString("CODVOL");
			BigDecimal qtdneg = DynamicVO.asBigDecimal("QTDNEG");
			BigDecimal sequencia = DynamicVO.asBigDecimal("SEQUENCIA");
			BigDecimal vlrtot = DynamicVO.asBigDecimal("VLRTOT");
			BigDecimal vlrunit = DynamicVO.asBigDecimal("VLRUNIT");
			String reserva = DynamicVO.asString("RESERVA");
			BigDecimal atualestoque = DynamicVO.asBigDecimal("ATUALESTOQUE");
			br.com.sankhya.jape.vo.DynamicVO tgfcab = getTGFCAB(nunotaDevolucao);
			
			insereItemNaNotaDevolucao(nunotaDevolucao, tgfcab.asBigDecimal("CODEMP"), localDestino, codprod, codvol, qtdneg, sequencia, vlrtot, vlrunit, reserva, atualestoque, localorig);
			
			}
	
		} catch (Exception e) {
			salvarException(
					"[listaItensNotaModelo] Nao foi possivel pegar a lista de itens da nota: "+nunotaModelo
							+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void insereItemNaNotaDevolucao(BigDecimal nunotaDev, BigDecimal empresa, BigDecimal local, BigDecimal produto, String volume, BigDecimal qtdneg, BigDecimal sequencia,
			BigDecimal vlrtot, BigDecimal vlrunit, String reserva, BigDecimal atualestoque, BigDecimal localDestino) {
		
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("ItemNota");
			DynamicVO VO = (DynamicVO) NPVO;
			
			DynamicVO tgfpro = getTGFPRO(produto);
		
			VO.setProperty("NUNOTA", nunotaDev);
			VO.setProperty("CODEMP", empresa);
			VO.setProperty("CODLOCALORIG", localDestino);
			VO.setProperty("CODPROD", produto);
			VO.setProperty("CODVOL", volume);
			VO.setProperty("QTDNEG", qtdneg);
			VO.setProperty("SEQUENCIA", sequencia);
			VO.setProperty("VLRTOT", vlrtot);
			VO.setProperty("VLRUNIT", vlrunit);
			VO.setProperty("RESERVA", reserva);
			VO.setProperty("ATUALESTOQUE", new BigDecimal(0));
			VO.setProperty("CODLOCALDEST", local);
			VO.setProperty("CODCFO", new BigDecimal(1415));
			VO.setProperty("CODTRIB", new BigDecimal(60));
			VO.setProperty("USOPROD", tgfpro.asString("USOPROD"));
			
			dwfFacade.createEntity("ItemNota", (EntityVO) VO);
			
		} catch (Exception e) {
			salvarException(
					"[insereItemNaNotaDevolucao] Nao foi possivel inserir itens da nota: "+nunotaDev
							+ e.getMessage() + "\n" + e.getCause());
		}
		
		
	}
	
	private void excluirNota(BigDecimal nunota) throws Exception {

		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeEntity("CabecalhoNota", new Object[] { nunota });

		} catch (Exception e) {
			salvarException("[excluirNota] Nao foi possivel excluir a nota! "+e.getMessage()+"\n"+e.getCause());
		}
	}

	private void cancelarOS(BigDecimal numos) {
		try {

			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("OrdemServico", "this.NUMOS=?", new Object[] { numos }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("SITUACAO", "F");
				VO.setProperty("DTFECHAMENTO", TimeUtils.getNow());
				VO.setProperty("CODUSUFECH", getUsuLogado());
				VO.setProperty("DHFECHAMENTOSLA", TimeUtils.getNow());
				VO.setProperty("CODCOS", new BigDecimal(5));
				VO.setProperty("DESCRICAO", "OS Cancelada através do botão \"Cancelar Abastecimento\" localizado na tela instalações!");

				itemEntity.setValueObject(NVO);

			}
		} catch (Exception e) {
			salvarException("[cancelarOS] Nao foi possivel cancelar a OS! "+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void cancelarSubOS(BigDecimal numos) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("ItemOrdemServico", "this.NUMOS=?", new Object[] { numos }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("CODSIT", new BigDecimal(5));
				VO.setProperty("HRINICIAL", new BigDecimal(0));
				VO.setProperty("HRFINAL", new BigDecimal(0));
				VO.setProperty("INICEXEC", TimeUtils.getNow());
				VO.setProperty("TERMEXEC", TimeUtils.getNow());
				VO.setProperty("SOLUCAO", "OS Cancelada através do botão \"Cancelar Abastecimento\" localizado na tela instalações!");

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			salvarException("[cancelarSubOS] Nao foi possivel cancelar a sub-OS! "+numos+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void excluirRetornoAbastecimento(BigDecimal idretorno) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_RETABAST",
					"this.ID=?", new Object[] { idretorno }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("STATUS", "4");

				itemEntity.setValueObject(NVO);
			}

			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_ITENSRETABAST", "this.ID=?",new Object[] {idretorno}));
			//dwfFacade.removeByCriteria(new FinderWrapper("AD_RETABAST", "this.ID=?",new Object[] {idretorno}));

		} catch (Exception e) {
			salvarException("[excluirRetornoAbastecimento] Nao foi possivel excluir o retorno de abastecimento! id retorno: "+idretorno+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void insertTcsrus(BigDecimal numos, BigDecimal codusurel) throws Exception {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();

			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("RelacionamentoUsuario");

			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("CODUSU", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("CODUSUREL", codusurel);
			VO.setProperty("TIPO", "G");
			VO.setProperty("VINCULO", "S");
			VO.setProperty("LIDERIMEDIATO", "N");
			
			dwfFacade.createEntity("RelacionamentoUsuario", (EntityVO) VO);
			
		} catch (Exception e) {
			salvarException("[insertTcsrus] não foi possível alterar usuário numos:" + numos + "\n" + e.getMessage()
			+ "\n" + e.getCause());

		}
	}

	private DynamicVO getTgfVar(BigDecimal nunota) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("CompraVendavariosPedido");
		DynamicVO VO = DAO.findOne("NUNOTAORIG=? AND SEQUENCIA=1", new Object[] { nunota });
		return VO;
	}
	
	private DynamicVO getTGFPRO(BigDecimal codprod) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?", new Object[] { codprod });
		return VO;
	}
	
	private DynamicVO getTSIEMP(BigDecimal empresa) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Empresa");
		DynamicVO VO = DAO.findOne("CODEMP=?", new Object[] { empresa });
		return VO;
	}

	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
		codUsuLogado = ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID();
		return codUsuLogado;
	}
	
	private DynamicVO getTGFCAB(BigDecimal nunota) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("CabecalhoNota");
		DynamicVO VO = DAO.findOne("NUNOTA=?", new Object[] { nunota });
		return VO;
	}
	
	private DynamicVO getTcsite(BigDecimal NumOs) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("ItemOrdemServico");
		DynamicVO VO = DAO.findOne("NUMOS=?", new Object[] { NumOs });
		return VO;
	}
	
	public void totalizaImpostos(BigDecimal nunota) throws Exception{
        ImpostosHelpper impostos = new ImpostosHelpper();
        impostos.carregarNota(nunota);
        impostos.setForcarRecalculo(true);
        impostos.calcularTotalItens(nunota, true);
        impostos.totalizarNota(nunota);
        impostos.calcularImpostos(nunota);
        impostos.salvarNota();
	}
	
	private boolean validaSeOhPedidoEstaEmOrdemDeCarga(BigDecimal nunota) {
		boolean valida = false;
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT CASE WHEN ORDEMCARGA IS NOT NULL THEN 1 ELSE 0 END AS QTD FROM TGFCAB WHERE nunota="+nunota);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("QTD");
				if (count == 0) {
					valida = true;
				}
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		return valida;
	}
	
	private void salvarException(String mensagem) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("OBJETO", "btn_cancelarAbastecimento");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);
			
			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);
			
		} catch (Exception e) {
			//aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! "+e.getMessage());
		}
	}
	
	
}
