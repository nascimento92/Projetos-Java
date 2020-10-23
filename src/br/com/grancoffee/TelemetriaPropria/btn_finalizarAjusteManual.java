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
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btn_finalizarAjusteManual implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {

		boolean confirmarSimNao = arg0.confirmarSimNao("ATENÇÃO!", "O sistema agendará o ajuste, continuar?", 1);
		if (confirmarSimNao) {
			start(arg0);
			arg0.setMensagemRetorno("Agendamento concluido</b>!");
		}
	}

	private void start(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		Object id = linhas[0].getCampo("ID");
		Object patrimonio = linhas[0].getCampo("CODBEM");
		Object ajustado = linhas[0].getCampo("AJUSTADO");

		if ("S".equals(ajustado)) {
			arg0.mostraErro("<b>Erro! Teclas já ajustadas</b>");
		} else {
			salvarTeclas(id, patrimonio);
		}
	}

	private void salvarTeclas(Object id, Object patrimonio) {
		try {

			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_ITENSAJUSTESMANUAIS",
					"this.ID=? AND this.CODBEM=? ", new Object[] { id, patrimonio }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);
				
				if(DynamicVO.asBigDecimal("SALDOAPOS")==null) {
					DynamicVO.setProperty("QTDAJUSTE", new BigDecimal(0));
					itemEntity.setValueObject((EntityVO) DynamicVO);
				}

				if(DynamicVO.asBigDecimal("QTDAJUSTE").intValue()!=0) {
								
					inserirSolicitacaoDeAjuste(
							DynamicVO.asString("CODBEM"),
							DynamicVO.asString("TECLA"),
							DynamicVO.asBigDecimal("CODPROD"),
							DynamicVO.asBigDecimal("CAPACIDADE"),
							DynamicVO.asBigDecimal("NIVELPAR"),
							DynamicVO.asBigDecimal("SALDOANTES"),
							DynamicVO.asBigDecimal("QTDAJUSTE"),
							id,
							DynamicVO.asString("MOTIVO"));
										
				}
				
				DynamicVO.setProperty("AJUSTADO", "S");
				itemEntity.setValueObject((EntityVO) DynamicVO);
			}

		} catch (Exception e) {
			System.out.println("## [btn_finalizarAjusteManual] ## - Não foi possivel cadastrar a solicitação de ajuste!");
			e.getMessage();
			e.getCause();
			e.printStackTrace();
		}
	}
	
	private void inserirSolicitacaoDeAjuste(String codbem, String tecla, BigDecimal produto, BigDecimal capacidade,
			BigDecimal nivelpar, BigDecimal saldoAtual, BigDecimal diferenca, Object idObjeto, String motivo) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("GCSolicitAjuste");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("CODBEM", codbem);
			VO.setProperty("CODUSU", getUsuLogado());
			VO.setProperty("TECLA", tecla);
			VO.setProperty("CODPROD", produto);
			VO.setProperty("CAPACIDADE", capacidade);
			VO.setProperty("NIVELPAR", nivelpar);
			VO.setProperty("SALDOANTERIOR", saldoAtual);
			VO.setProperty("QTDAJUSTE", diferenca);
			VO.setProperty("MANUAL", "S");
			VO.setProperty("SALDOFINAL", saldoAtual.add(diferenca));
			VO.setProperty("IDABASTECIMENTO", idObjeto);
			VO.setProperty("AD_DTSOLICIT", TimeUtils.getNow());
			
			if(motivo!=null) {
				VO.setProperty("OBSERVACAO", motivo);
			}else {
				VO.setProperty("OBSERVACAO", "Ajuste Manual");
			}

			dwfFacade.createEntity("GCSolicitAjuste", (EntityVO) VO);

		} catch (Exception e) {
			System.out.println("## [btn_finalizarAjusteManual] ## - Não foi possivel cadastrar a solicitação de ajuste!");
			e.getMessage();
			e.getCause();
			e.printStackTrace();
		}
	}
	
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
		codUsuLogado = ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID();
		return codUsuLogado;
	}

}
