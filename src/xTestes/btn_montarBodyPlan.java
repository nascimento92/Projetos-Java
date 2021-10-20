package xTestes;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btn_montarBodyPlan implements AcaoRotinaJava {
	
	/**
	 * 18/05/21 15:47 No momento que for parar a sincronização das teclas com a aba planograma da tela instaação, será necessário alterar esse objeto
	 */
	
	int varTeste;
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Object codbem = arg0.getParam("CODBEM");	
		arg0.setMensagemRetorno(montarBody(codbem));
	}

	public String montarBody(Object codbem) throws Exception {
		
		String head="{\"planogram\":{\"items_attributes\": [";
		String bottom="]}}";
		
		String body = "";
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		Collection<?> parceiro = dwfEntityFacade
				.findByDynamicFinder(new FinderWrapper("teclas", "this.CODBEM = ? ", new Object[] { codbem }));

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			
			String teclaAlternativa = DynamicVO.asString("TECLAALT");
			String tecla = DynamicVO.asBigDecimal("TECLA").toString();
			String name = "";
			
			if(teclaAlternativa!=null) {
				name = teclaAlternativa;
			}else {
				name = tecla;
			}
			
			BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
			
			
			if(Iterator.hasNext()) {
				body=body+"{"+
						"\"type\": \"Coil\","+
						"\"name\": \""+name+"\","+
						"\"good_id\": "+getGoodId(produto)+","+
						"\"capacity\": "+DynamicVO.asBigDecimal("AD_CAPACIDADE").toString()+","+
						"\"par_level\": "+DynamicVO.asBigDecimal("AD_NIVELPAR").toString()+","+
						"\"alert_level\": "+DynamicVO.asBigDecimal("AD_NIVELALERTA").toString()+","+
						"\"desired_price\": "+DynamicVO.asBigDecimal("VLRFUN").add(DynamicVO.asBigDecimal("VLRPAR")).toString()+","+
						"\"logical_locator\": "+DynamicVO.asBigDecimal("TECLA").toString()+","+
						"\"status\": \"active\""
						+ "},";
			}else {
				body=body+"{"+
						"\"type\": \"Coil\","+
						"\"name\": \""+DynamicVO.asBigDecimal("TECLA").toString()+"\","+
						"\"good_id\": "+getGoodId(produto)+","+
						"\"capacity\": "+DynamicVO.asBigDecimal("AD_CAPACIDADE").toString()+","+
						"\"par_level\": "+DynamicVO.asBigDecimal("AD_NIVELPAR").toString()+","+
						"\"alert_level\": "+DynamicVO.asBigDecimal("AD_NIVELALERTA").toString()+","+
						"\"desired_price\": "+DynamicVO.asBigDecimal("VLRFUN").add(DynamicVO.asBigDecimal("VLRPAR")).toString()+","+
						"\"logical_locator\": "+DynamicVO.asBigDecimal("TECLA").toString()+","+
						"\"status\": \"active\""
						+ "}";
			}
		
		}
		
		return head+body+bottom;

	}
	
	public BigDecimal getGoodId(BigDecimal produto) throws Exception {
		BigDecimal id = null;
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?",new Object[] { produto });
		BigDecimal idVerti = VO.asBigDecimal("AD_IDPROVERTI");
		
		if(idVerti==null) {
			id = new BigDecimal(179707);
		}else {
			id = idVerti;
		}
		
		return id;
	}

}
