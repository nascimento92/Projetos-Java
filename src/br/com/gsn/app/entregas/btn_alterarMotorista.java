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

public class btn_alterarMotorista implements AcaoRotinaJava{

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		String idMotorista = (String) arg0.getParam("ID");
		String veiculo = (String) arg0.getParam("VEICULO");
		
		for(int i=0; i<linhas.length; i++) {
			registrarMotorista(linhas[i], new BigDecimal(idMotorista), new BigDecimal(veiculo));
		}
		
		arg0.setMensagemRetorno("Motorista/Veiculo alterado!");
	}
	
	private void registrarMotorista(Registro linhas, BigDecimal idMotorista, BigDecimal veiculo) {
		try {
			
			Object oc = linhas.getCampo("ORDEMCARGA");
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("OrdemCarga",
					"this.ORDEMCARGA=?", new Object[] { oc }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				if(idMotorista!=null) {
					VO.setProperty("AD_APPMOTO", idMotorista);
				}
				
				if(veiculo!=null) {
					VO.setProperty("CODVEICULO", veiculo);
				}

				itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			throw new Error("ops "+ e.getCause());
		}
	}

}
