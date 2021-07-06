package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
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
			
			boolean confirmarSimNao = false;
			boolean confirmarNotaJaFaturada = false;
			
			if ("1".equals(status)) {
				if (nunota == null && numos == null) {
					confirmarSimNao = arg0.confirmarSimNao("Atenção", "A solicitação ainda não possui um pedido nem uma OS, mesmo assim deseja cancelar?", 1);
				}else {
					confirmarSimNao = arg0.confirmarSimNao("Atenção", "O pedido <b>"+nunota+"</b> será excluido do portal de vendas, e a OS <b>"+numos+"</b> será cancelada, continuar?", 1);
				}
				
				if(confirmarSimNao) {
					if (nunota != null && numos != null) {
						
						DynamicVO tgfVar = getTgfVar(nunota);
						if (tgfVar != null) {
							confirmarNotaJaFaturada = arg0.confirmarSimNao("Atenção", "Pedido já faturada, deseja cancelar apenas a OS?", 1);
							
							if(confirmarNotaJaFaturada) {
								DynamicVO tabelaTcsite = getTcsite(numos);
								BigDecimal codusurel = tabelaTcsite.asBigDecimal("CODUSU");
								insertTcsrus(numos,codusurel);
								cancelarSubOS(numos);
								cancelarOS(numos);		
							}
						}else {
							DynamicVO tabelaTcsite = getTcsite(numos);
							BigDecimal codusurel = tabelaTcsite.asBigDecimal("CODUSU");
							insertTcsrus(numos,codusurel);
							excluirNota(nunota);
							cancelarSubOS(numos);
							cancelarOS(numos);
						}
						
					}else
						if(nunota != null && numos == null) {
							
							DynamicVO tgfVar = getTgfVar(nunota);
							if (tgfVar != null) {
								confirmarNotaJaFaturada = arg0.confirmarSimNao("Atenção", "Pedido já faturada, será apenas cancelada a visita, continuar?", 1);
							}else {
								excluirNota(nunota);
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
					excluirRetornoAbastecimento(numos, nunota);
					linhas[0].setCampo("STATUS", "4");
			        linhas[0].setCampo("AD_DTCANCELAMENTO", TimeUtils.getNow());
			        linhas[0].setCampo("AD_CODUSUCANCEL", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
				}
			}else
				if("4".equals(status)) {
					arg0.mostraErro("<b>Abastecimento jfoi cancelado!</b>");
				}else {
					arg0.mostraErro("<b>Abastecimento jfoi realizado nposscancela-lo!</b>");
				}
		}
		if(cont>0) {
			arg0.setMensagemRetorno("Visita Cancelada!");
		}else {
			arg0.setMensagemRetorno("Ops, algo deu errado!");
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
	
	private void excluirRetornoAbastecimento(BigDecimal numos, BigDecimal nunota) {
		try {
			
			JapeWrapper DAO = JapeFactory.dao("AD_RETABAST");
			DynamicVO VO = null;
			
			if(numos!=null) {
				VO = DAO.findOne("NUMOS=?",new Object[] { numos });
			}else {
				VO = DAO.findOne("NUNOTA=?",new Object[] { nunota });
			}
			
			BigDecimal id = VO.asBigDecimal("ID");
			
			if(id!=null) {
				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				dwfFacade.removeByCriteria(new FinderWrapper("AD_ITENSRETABAST", "this.ID=?",new Object[] {id}));
				dwfFacade.removeByCriteria(new FinderWrapper("AD_RETABAST", "this.ID=?",new Object[] {id}));
			}
			
			
		} catch (Exception e) {
			salvarException("[excluirRetornoAbastecimento] Nao foi possivel excluir o retorno de abastecimento! "+e.getMessage()+"\n"+e.getCause());
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
			VO.setProperty("VINCULO", "s");
			VO.setProperty("LIDERIMEDIATO", "N");
			
			dwfFacade.createEntity("RelacionamentoUsuario", (EntityVO) VO);
			
		} catch (Exception e) {
			salvarException("[insertTcsrus] não foi possível alterar usuário numos:" + numos + "\n" + e.getMessage()
			+ "\n" + e.getCause());

		}
	}

	private DynamicVO getTgfVar(BigDecimal nunota) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("CompraVendavariosPedido");
		DynamicVO VO = DAO.findOne("NUNOTAORIG=? AND SEQUENCIAORIG=1", new Object[] { nunota });
		return VO;
	}

	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
		codUsuLogado = ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID();
		return codUsuLogado;
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
	
	private DynamicVO getTcsite(BigDecimal NumOs) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("ItemOrdemServico");
		DynamicVO VO = DAO.findOne("NUMOS=?", new Object[] { NumOs });
		return VO;
	}
}
