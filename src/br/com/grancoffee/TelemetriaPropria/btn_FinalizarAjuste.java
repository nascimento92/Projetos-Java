package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

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

public class btn_FinalizarAjuste implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		start(arg0);		
	}
	
	private void start(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		if(linhas.length>1) {
			arg0.mostraErro("Selecione apenas uma linha!");
		}else {
			BigDecimal id = (BigDecimal) linhas[0].getCampo("ID");
			//inserir Validação para prosseguir apenas se for um ajuste de inventário manual, validar pelo campo
			pegarTeclas(id,arg0);
		}
	}
		
	private void pegarTeclas(BigDecimal idObjeto, ContextoAcao arg0) throws Exception {

		int cont = 0;

		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(
				new FinderWrapper("GCItensAbastecimento", "this.IDABASTECIMENTO = ? ", new Object[] { idObjeto }));

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			BigDecimal qtdajuste = DynamicVO.asBigDecimal("QTDAJUSTE");
			String tipoAjuste = DynamicVO.asString("TIPOAJUSTE");
			
			//validar para rodar apenas se pelo menos uma tecla tiver sido preenchida, fazer um count
			
			if (qtdajuste != null) {
				if(qtdajuste.intValue()!=0) {
					if(tipoAjuste!=null) {
						String tecla = DynamicVO.asString("TECLA");
						BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
						BigDecimal capacidade = DynamicVO.asBigDecimal("CAPACIDADE");
						BigDecimal nivelpar = DynamicVO.asBigDecimal("NIVELPAR");
						BigDecimal saldoAtual = DynamicVO.asBigDecimal("SALDOATUAL");
						String codbem = DynamicVO.asString("CODBEM");

						
						BigDecimal valor = null;
						if ("1".equals(tipoAjuste)) {// inserir
							valor = qtdajuste;
						} else

						if ("2".equals(tipoAjuste)) { // retirar
							valor = qtdajuste.negate();
						} else {
							valor = qtdajuste;
							DynamicVO.setProperty("TIPOAJUSTE", "1");
						}

						String obs = DynamicVO.asString("OBSAJUSTE");
						if (obs == null) {
							obs = "Ajuste de Inventário Manual.";
						}

						inserirSolicitacaoDeAjuste(tecla, produto, capacidade, nivelpar, saldoAtual, codbem, valor, obs,idObjeto);
						cont++;
					}else {
						DynamicVO.setProperty("QTDAJUSTE", new BigDecimal(0));
					}
					
				}else {
					DynamicVO.setProperty("QTDAJUSTE", new BigDecimal(0));
					DynamicVO.setProperty("OBSAJUSTE", "");
					DynamicVO.setProperty("TIPOAJUSTE", "");
				}

			} else {
				DynamicVO.setProperty("QTDAJUSTE", new BigDecimal(0));
				DynamicVO.setProperty("OBSAJUSTE", "");
				DynamicVO.setProperty("TIPOAJUSTE", "");
			}
			
			DynamicVO.setProperty("AJUSTADO", "S");
			itemEntity.setValueObject((EntityVO) DynamicVO);
		}

		if (cont > 0) {
			arg0.setMensagemRetorno("Ajuste Agendado!");
		}
	}
	
	private void inserirSolicitacaoDeAjuste(String tecla, BigDecimal produto, BigDecimal capacidade,
			BigDecimal nivelpar, BigDecimal saldoAtual, String codbem, BigDecimal valor, String obs,BigDecimal idabast) {
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
			VO.setProperty("QTDAJUSTE", valor);
			VO.setProperty("MANUAL", "S");
			VO.setProperty("OBSERVACAO", obs);
			VO.setProperty("SALDOFINAL", saldoAtual.add(valor));
			//VO.setProperty("IDABASTECIMENTO", idabast);

			dwfFacade.createEntity("GCSolicitAjuste", (EntityVO) VO);

		} catch (Exception e) {
			System.out
					.println("## [btn_FinalizarAjuste] ## - Não foi possivel cadastrar a solicitação de ajuste!");
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
