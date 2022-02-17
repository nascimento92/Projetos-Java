package br.com.gsn.app.entregas;

import java.math.BigDecimal;
import java.sql.ResultSet;
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
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import br.com.sankhya.ws.ServiceContext;

/**
 * 
 * @author fernando.silva
 * @version 1.3 20/10/2021 - Adicionado campos a ser inseridos na tabela (DTEXP,
 *          NOMEROTA);
 */

public class btn_vincularMotorista implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		String idMotorista = (String) arg0.getParam("ID");
		String veiculo = (String) arg0.getParam("VEICULO");
		Timestamp dtExp = (Timestamp) arg0.getParam("DTEXP");
		String rota = (String) arg0.getParam("NOMEROTA");
		
		//TODO :: INSERIR TRAVA PARA VALIDAR SE A OC JÁ ESTA INTEGRADA

		for (int i = 0; i < linhas.length; i++) {
			registraDadosNaOC(linhas[i], new BigDecimal(veiculo), rota);
		}

		for (int i = 0; i < linhas.length; i++) {
			BigDecimal oc = (BigDecimal) linhas[i].getCampo("ORDEMCARGA");
			BigDecimal empresa = (BigDecimal) linhas[i].getCampo("CODEMP");

			descobriPedidosParaSeremIntegrados(oc, empresa, new BigDecimal(idMotorista), new BigDecimal(veiculo), dtExp);
		}

		if (linhas.length > 0) {
			arg0.setMensagemRetorno("Motorista / Veículo vinculados!");
			chamaPentaho();
		} else {
			throw new Error("<br/><br/><b>Selecione uma ou mais Ordens de carga!</b><br/></b><br/>");
		}

	}

	private void registraDadosNaOC(Registro linhas, BigDecimal veiculo, String rota) {
		try {

			linhas.setCampo("CODVEICULO", veiculo); // será salvo no pedido CODVEICULO
			linhas.setCampo("AD_NOMEROTA", rota);
			linhas.setCampo("AD_INTEGRADO", "S");
		} catch (Exception e) {
			throw new Error("ops " + e.getCause());
		}
	}

	// AD_DTEXP
	// CODVEICULO
	// AD_MOTENTREGA

	private void descobriPedidosParaSeremIntegrados(BigDecimal oc, BigDecimal empresa, BigDecimal idMotorista, BigDecimal veiculo, Timestamp data) {

		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT NRO_UNICO FROM GC_LISTA_ENTREGAS WHERE ORDEMCARGA="+oc+" AND CODEMP="+empresa);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				BigDecimal nroUnico = contagem.getBigDecimal("NRO_UNICO");
				alteraDadosCab(nroUnico,idMotorista,veiculo,data);
				salvarNaIntegracao(nroUnico, oc, empresa);
			}

		} catch (Exception e) {
			// TODO: handle exception
		}

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
