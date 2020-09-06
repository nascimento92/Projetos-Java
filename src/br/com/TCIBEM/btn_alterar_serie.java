package br.com.TCIBEM;

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

public class btn_alterar_serie implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		
		if(linhas.length==1) {
			String serie = (String) arg0.getParam("SERIE");
			start(linhas,serie,arg0);
		}else {
			arg0.setMensagemRetorno("SELECIONE APENAS UM BEM !");
		}
		
	}
	
	public void start(Registro[] linhas, String serie, ContextoAcao arg0) {
		String patrimonio = (String) linhas[0].getCampo("CODBEM");	
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("Imobilizado",
					"this.CODBEM=? ", new Object[] { patrimonio }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("DESCRBEM", serie);

				itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			arg0.setMensagemRetorno("NAO FOI POSSIVEL ALTERAR A SERIE!\n\n"+e.getMessage());
		}		
	}

}
