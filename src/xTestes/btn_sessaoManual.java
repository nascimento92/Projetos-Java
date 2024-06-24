package xTestes;

import java.util.Collection;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class btn_sessaoManual implements AcaoRotinaJava{
	

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		teste(arg0);
	}
	
	private void teste(ContextoAcao arg0) throws Exception {
		SessionHandle hnd = null;
		
		String x = "";

		try {
			hnd = JapeSession.open();

			hnd.execWithTX( new JapeSession.TXBlock(){
				public void doWithTx() throws Exception{
				
					JapeWrapper dao = JapeFactory.dao("GCInstalacao");
					Collection<DynamicVO> lista = dao.find("this.CODBEM=?", new Object[] {"GCETESTE"});
					for(DynamicVO item : lista) {
						arg0.setMensagemRetorno("Patrimonio: " + item.asString("CODBEM"));;
					}
					
				}
			});
		} finally {
			JapeSession.close(hnd);
		}

	}
	

}
