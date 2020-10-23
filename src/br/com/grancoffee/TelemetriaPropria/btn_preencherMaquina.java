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
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btn_preencherMaquina implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		boolean confirmarSimNao = arg0.confirmarSimNao("ATENÇÃO!", "O sistema preencherá os valores necessários para <b>Completar a Máquina</b>, continuar?", 1);
		if(confirmarSimNao) {
			start(arg0);
			arg0.setMensagemRetorno("Dados carregados, clicar no botão <b>Finalizar Ajuste</b>!");
		}		
	}
	
	private void start(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		Object id = linhas[0].getCampo("ID");
		Object patrimonio = linhas[0].getCampo("CODBEM");
		Object ajustado = linhas[0].getCampo("AJUSTADO");
		
		if("S".equals(ajustado)) {
			arg0.mostraErro("<b>Erro! Teclas já ajustadas</b>");
		}else {
			preencherMaquina(id, patrimonio);
		}
		
	}
	
	private void preencherMaquina(Object id, Object patrimonio) {
		try {

			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_ITENSAJUSTESMANUAIS",
					"this.ID=? AND this.CODBEM=? ", new Object[] { id, patrimonio }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;
				
				BigDecimal nivelPar = VO.asBigDecimal("NIVELPAR");
				BigDecimal saldoAntes = VO.asBigDecimal("SALDOANTES");
				
				VO.setProperty("QTDAJUSTE", nivelPar.subtract(saldoAntes));

				itemEntity.setValueObject(NVO);
			}

		} catch (Exception e) {
			// TODO: handle exception
		}
	}

}
