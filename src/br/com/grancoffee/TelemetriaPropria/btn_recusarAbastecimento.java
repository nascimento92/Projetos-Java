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

public class btn_recusarAbastecimento implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();

		String campo = (String) linhas[0].getCampo("AJUSTADO");

		if ("S".equals(campo)) {
			arg0.mostraErro("Abastecimento já ajustado!");
		} else {
			boolean confirmarSimNao = arg0.confirmarSimNao("Atenção!",
					"Todas as informações digitadas pelo promotor serão <b>recusadas</b> e o inventário <b>não será ajustado</b>, continuar?",
					1);

			if (confirmarSimNao) {

				Object idObjeto = linhas[0].getCampo("IDABASTECIMENTO");
				pegarTeclas(idObjeto, arg0);
				arg0.setMensagemRetorno("Finalizado!");
			}
		}
	}
	
	private void pegarTeclas(Object idObjeto,ContextoAcao arg0) throws Exception {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("GCItensAbastecimento", "this.IDABASTECIMENTO = ? ", new Object[] { idObjeto }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				
				DynamicVO.setProperty("QTDAJUSTE", new BigDecimal(0));
				DynamicVO.setProperty("AJUSTADO", "S");
				itemEntity.setValueObject((EntityVO) DynamicVO);
			}
			
		} catch (Exception e) {
			System.out.println("## [btn_recusarAbastecimento] ## - Não foi possivel salvar as informações nas teclas");
		}
	}

}
