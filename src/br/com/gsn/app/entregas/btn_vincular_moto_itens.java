package br.com.gsn.app.entregas;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;

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
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import br.com.sankhya.ws.ServiceContext;

public class btn_vincular_moto_itens implements AcaoRotinaJava{
	int qtd = 0;
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		String idMotorista = (String) arg0.getParam("ID");
		String veiculo = (String) arg0.getParam("VEICULO");
		Timestamp dtExp = (Timestamp) arg0.getParam("DTEXP");
		
		for (int i = 0; i < linhas.length; i++) {
			
			BigDecimal nrounico = null;
			Integer x = (Integer) linhas[i].getCampo("NRO_UNICO");
			nrounico = new BigDecimal(x);
			
			registrarIntegracao(nrounico, new BigDecimal(idMotorista), new BigDecimal(veiculo), dtExp);
		}
		
		if(qtd>0) {
			arg0.setMensagemRetorno("Motorista/Veiculo vinculado!");	
		}else {
			throw new Error("<br/><br/><b>Selecione uma ou mais entregas!</b><br/></b><br/>");
		}
		
		chamaPentaho();
	}
	
	private void registrarIntegracao(BigDecimal nrounico, BigDecimal idMotorista, BigDecimal veiculo, Timestamp data) throws Exception {
		
		if(nrounico!=null) {
			DynamicVO tgfcab = getTGFCAB(nrounico);
			if(tgfcab!=null) {
				BigDecimal oc = tgfcab.asBigDecimal("ORDEMCARGA");
				BigDecimal empresa = tgfcab.asBigDecimal("CODEMP");
				String status = tgfcab.asString("AD_STATUSENTREGA");
				
				if(status==null) {
					alteraDadosCab(nrounico, idMotorista, veiculo, data);
					salvarNaIntegracao(nrounico,oc,empresa);
					qtd++;
					
				}else {
					throw new Error("<br/><b>OPS!</b><br/><br/>A entrega já está integrada!<br/><br/>");
				}
				
				
			}
		}
		
	}
	
	private DynamicVO getTGFCAB(BigDecimal nunota) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("CabecalhoNota");
		DynamicVO VO = DAO.findOne("NUNOTA=?",new Object[] { nunota });
		return VO;
	}
	
	private void alteraDadosCab(BigDecimal nrounico, BigDecimal idMotorista, BigDecimal veiculo, Timestamp data) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("CabecalhoNota",
					"this.NUNOTA=?", new Object[] { nrounico }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("AD_DTEXP", data);
				VO.setProperty("CODVEICULO", veiculo);
				VO.setProperty("AD_MOTENTREGA", idMotorista);

				itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	private void salvarNaIntegracao(BigDecimal nrounico, BigDecimal oc, BigDecimal empresa) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_INTENTREGAS");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("NUNOTA", nrounico);
			VO.setProperty("DTSOLICIT", TimeUtils.getNow());
			VO.setProperty("TIPO", "I");
			VO.setProperty("ORDEMCARGA", oc);
			VO.setProperty("CODEMP", empresa);
			VO.setProperty("CODUSU",  ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
			
			dwfFacade.createEntity("AD_INTENTREGAS", (EntityVO) VO);
			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	private void chamaPentaho() {

		try {

			String site = (String) MGECoreParameter.getParameter("PENTAHOIP");
			String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/APPS/APP Entregas/Prod/Entregas/";
			String objName = "T-Cadastrar_entregas";

			si.runTrans(path, objName);

		} catch (Exception e) {
			e.getMessage();
		}
	}

}
