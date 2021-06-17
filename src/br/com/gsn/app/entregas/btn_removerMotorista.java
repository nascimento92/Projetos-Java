package br.com.gsn.app.entregas;

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

public class btn_removerMotorista implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();

		boolean confirmarSimNao = arg0.confirmarSimNao("Atenção", "O vinculo entre Motorista/Veiculo/O.C será desfeito, continuar?", 0);
		if(confirmarSimNao) {

			for (int i = 0; i < linhas.length; i++) {
				removerMotorista(linhas[i]);
			}
		}


		arg0.setMensagemRetorno("Motorista/Veiculo removidos!");
	}

	private void removerMotorista(Registro linhas) {
		try {
			Object oc = linhas.getCampo("ORDEMCARGA");

			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("OrdemCarga", "this.ORDEMCARGA=?", new Object[] { oc }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("AD_APPMOTO", null);
				VO.setProperty("CODVEICULO", new BigDecimal(0));

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			throw new Error("ops " + e.getCause());
		}
	}

}
