package br.com.buttons;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btn_senhas_pec_novo implements AcaoRotinaJava{
	
	int count = 0;
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		verificaRegistrosComSenhaNula();
		
		if(count>0) {
			arg0.setMensagemRetorno("Foram atualizados "+count+" contatos!.");
		}
	}
	
	private void verificaRegistrosComSenhaNula() {
		
		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT CODUSU FROM PECUSU WHERE SENHA IS NULL");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				BigDecimal codusu = contagem.getBigDecimal("CODUSU");
				atualizaSenha(codusu);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}
	
	private void atualizaSenha(BigDecimal codusu) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("PecUsuario",
					"this.CODUSU=?", new Object[] { codusu }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;
				
				String senhatemp = VO.asString("AD_SENHAPORTALTEMP");

				if(senhatemp!=null) {
					VO.setProperty("SENHA", senhatemp);
					count++;
				}

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

}
