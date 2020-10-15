package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceException;
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

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {

		if (arg0.getLinhas().length > 1) {
			arg0.mostraErro("<b>Selecione apenas uma linha!</b>");
		} else {
			Registro[] linhas = arg0.getLinhas();
			Object nunota = linhas[0].getCampo("NUNOTA");
			Object numos = linhas[0].getCampo("NUMOS");
			String status = (String) linhas[0].getCampo("STATUS");

			if ("1".equals(status)) {

				if (nunota != null && numos != null) {
					boolean confirmarSimNao = arg0.confirmarSimNao("Atenção!",
							"O pedido <b>" + nunota + "</b> será excluido do portal de vendas, e a OS <b>" + numos
									+ "</b> será cancelada, continuar?",
							1);

					if (confirmarSimNao) {
						start(linhas[0]);
					}
				} else {
					arg0.mostraErro("<b>Pedido de abastecimento ainda não foi gerado!</b>");
				}

			} else if ("4".equals(status)) {
				arg0.mostraErro("<b>Abastecimento já foi cancelado!</b>");
			}else {
				arg0.mostraErro("<b>Abastecimento já foi realizado não é possível cancela-lo!</b>");
			}
		}
	}

	private void start(Registro linhas) throws Exception {
		BigDecimal nunota = (BigDecimal) linhas.getCampo("NUNOTA");
		BigDecimal numos = (BigDecimal) linhas.getCampo("NUMOS");

		excluirNota(nunota);
		cancelarOS(numos);
		excluirRetornoAbastecimento(nunota);
		linhas.setCampo("STATUS", "4");
	}

	private void excluirNota(BigDecimal nunota) throws Exception {

		DynamicVO tgfVar = getTgfVar(nunota);
		if (tgfVar != null) {
			throw new PersistenceException(
					"<br/>O Pedido: <b>" + nunota + "</b> já foi faturado e gerou o número único: <b>"
							+ tgfVar.asBigDecimal("NUNOTA") + "</b> não é possível cancelar o abastecimento!");
		}

		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeEntity("CabecalhoNota", new Object[] { nunota });

		} catch (Exception e) {
			System.out
					.println("## [btn_cancelarAbastecimento] ## - Não foi possível excluir o pedido de abastecimento!");
			e.getMessage();
			e.getCause();
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

				itemEntity.setValueObject(NVO);

			}
		} catch (Exception e) {
			System.out.println("## [btn_cancelarAbastecimento] ## - Não foi possível cancelar a OS!");
			e.getMessage();
			e.getCause();
		}
	}
	
	private void excluirRetornoAbastecimento(BigDecimal nunota) {
		try {
			
			JapeWrapper DAO = JapeFactory.dao("AD_RETABAST");
			DynamicVO VO = DAO.findOne("NUNOTA=?",new Object[] { nunota });
			BigDecimal id = VO.asBigDecimal("ID");
			
			if(id!=null) {
				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				dwfFacade.removeByCriteria(new FinderWrapper("AD_ITENSRETABAST", "this.ID=?",new Object[] {id}));
				dwfFacade.removeByCriteria(new FinderWrapper("AD_RETABAST", "this.ID=?",new Object[] {id}));
			}
			
			
		} catch (Exception e) {
			System.out.println("## [btn_cancelarAbastecimento] ## - Não foi possível excluir o retorno do abastecimento!");
			e.getMessage();
			e.getCause();
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
}
