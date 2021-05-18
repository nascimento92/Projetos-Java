package br.com.grancoffee.TelemetriaPropria;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.Timer;

import com.sankhya.util.TimeUtils;

import Helpers.WSPentaho;
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
import br.com.sankhya.modelcore.util.MGECoreParameter;
import br.com.sankhya.ws.ServiceContext;

public class btn_novoEnviarPlanograma implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		Object patrimonio = linhas[0].getCampo("CODBEM");
		marcarBotaoPendente(patrimonio);
		
		Timer timer = new Timer(2000, new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				chamaPentaho();		
			}
		});
		timer.setRepeats(false);
		timer.start();
		
		chamaPentaho();
		arg0.setMensagemRetorno("Planograma Enviado!");
	}

	public void marcarBotaoPendente(Object patrimonio) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("GCInstalacao",
					"this.CODBEM=?", new Object[] {patrimonio}));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("AD_ENVIARPLANOGRAMA", "S");

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			salvarException("[marcarBotaoPendente] Nao foi possivel marcar o botao como pendente! codbem: "+patrimonio+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void chamaPentaho() {

		try {

			String site = (String) MGECoreParameter.getParameter("PENTAHOIP");;
			String Key = "Basic Z2FicmllbC5uYXNjaW1lbnRvOkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC_New/Transformation/Sankhya-Planograma/";
			String objName = "T-TF-Atualizar_planograma";

			si.runTrans(path, objName);

		} catch (Exception e) {
			salvarException("[chamaPentaho] nao foi possivel chamar o pentaho! "+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void salvarException(String mensagem) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "btn_novoEnviarPlanograma");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);
		} catch (Exception e) {
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}

}
