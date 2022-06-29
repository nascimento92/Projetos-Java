package br.com.TCIBEM;

import java.math.BigDecimal;
import java.sql.Timestamp;
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
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btn_ajustaEmpresa implements AcaoRotinaJava{
	
	int cont = 0;
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		String emp = (String) arg0.getParam("EMP");
		
		atualizaEmpresa(linhas[0], new BigDecimal(emp));
		
		if(cont > 0) {
			arg0.setMensagemRetorno("Bem alterado para empresa <b>"+emp+"</b>");
		}
	}
	
	private void atualizaEmpresa(Registro linhas, BigDecimal emp) {
		
		Timestamp datalimite = TimeUtils.buildData(8, 7, 2022);
		String codbem = (String) linhas.getCampo("CODBEM");
		
		if(TimeUtils.getNow().before(datalimite)) {
			
			try {
				
				EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
				Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("Imobilizado",
						"this.CODBEM=?", new Object[] { codbem }));
				for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
					PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
					EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
					DynamicVO VO = (DynamicVO) NVO;

					VO.setProperty("CODEMP", emp);

					itemEntity.setValueObject(NVO);
					cont++;
				}
			
			} catch (Exception e) {
				// TODO: handle exception
			}
			
		}else {
			throw new Error("A utilização deste botão alcançou a data limite, não é possível mais utiliza-lo !");
		}	
		
	}

}
