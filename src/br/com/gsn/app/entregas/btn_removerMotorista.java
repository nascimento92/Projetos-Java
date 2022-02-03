package br.com.gsn.app.entregas;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;

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
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;

public class btn_removerMotorista implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();

		boolean confirmarSimNao = arg0.confirmarSimNao("Atenção",
				"O vinculo entre Motorista/Veiculo/O.C será desfeito, continuar?", 0);
		if (confirmarSimNao) {

			for (int i = 0; i < linhas.length; i++) {
				
				//TODO::Valida se existe algum pedido não pendente.
				Integer oc =  (Integer) linhas[i].getCampo("ORDEMCARGA");
				if(verificaSeExistePedidosQueNaoEstaoPendentes(new BigDecimal(oc))) {
					throw new Error("<br/><br/><b>OPS!</b><br/> Existem pedidos em execução ou finalizados! Motorista não pode ser desvinculado da Ordem de Carga !<br/><br/></br/>");
				}
				
				removerMotorista(linhas[i]);
			}
		}
		
		if(linhas.length>0) {
			arg0.setMensagemRetorno("Motorista/Veiculo removidos!");
		}else {
			throw new Error("<br/><br/><b>Selecione uma ou mais Ordens de carga!</b><br/></b><br/>");
		}

		
		chamaPentaho();
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
				VO.setProperty("AD_INTEGRADO", "N");
				VO.setProperty("AD_NOMEROTA", null);

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			throw new Error("ops " + e.getCause());
		}
	}

	private void chamaPentaho() {

		try {

			String site = (String) MGECoreParameter.getParameter("PENTAHOIP");
			String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/APPS/APP Entregas/Prod/Entregas/";
			String objName = "T-Excluir_entregas";

			si.runTrans(path, objName);

		} catch (Exception e) {
			e.getMessage();
		}
	}
	
	private boolean verificaSeExistePedidosQueNaoEstaoPendentes(BigDecimal oc) {
		boolean valida = false;
		
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT COUNT(*) AS QTD FROM TGFCAB WHERE ORDEMCARGA=15119 AND AD_STATUSENTREGA <> '1'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("QTD");
				if (count >= 1) {
					valida = true;
				}
			}	
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		
		return valida;
	}

}
