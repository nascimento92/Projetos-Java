package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class evento_calculaRetornos implements EventoProgramavelJava {
	
	/**
	 * 11/05/2022 vs 1.3 - Inserido a funcionalidade para registrar o horário de alteração/inserção da linha.
	 */
	
	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		update(arg0);
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		update(arg0);
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		update(arg0);
	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		if(VO.asBigDecimal("QTD").intValue()==0) {
			VO.setProperty("QTD", new BigDecimal(1));
		}
		
		VO.setProperty("DTALTER", TimeUtils.getNow());
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		if(VO.asBigDecimal("QTD").intValue()==0) {
			throw new Error("Valor não pode ser zero, caso não houve retorno deste item, exclui-lo!");
		}
		
		VO.setProperty("DTALTER", TimeUtils.getNow());
	}


	private void salvaNovoValorTelaItens(BigDecimal codprod, BigDecimal id, String tecla, BigDecimal quantidade) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_ITENSRETABAST",
					"this.CODPROD=? AND this.ID=? AND this.TECLA=? ", new Object[] { codprod, id, tecla }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;
				
				VO.setProperty("QTDRETORNO", quantidade);

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			salvarException("[salvaNovoValorTelaItens] Nao foi possivel atualizar a quantidade de retorno! Id: "+id+" produto: "+codprod+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void update(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal id = VO.asBigDecimal("ID");
		
		BigDecimal codprod = VO.asBigDecimal("CODPROD");
		String tecla = VO.asString("TECLA");
		BigDecimal qtd = new BigDecimal(calculaRetornos(id,codprod,tecla));
		salvaNovoValorTelaItens(codprod,id,tecla,qtd);
		
	}
	
	
	private int calculaRetornos(BigDecimal id, BigDecimal codprod, String tecla) {
		int qtd=0;
		
		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT SUM(QTD) AS QTD FROM AD_PRODRETABAST WHERE ID=" + id + " AND CODPROD=" + codprod
					+ " AND TECLA='" + tecla + "'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				qtd = contagem.getInt("QTD");
			}
		} catch (Exception e) {
			salvarException("[calculaRetornos] Nao foi possivel calcular os retornos! Id: "+id+" produto: "+codprod+"\n"+e.getMessage()+"\n"+e.getCause());
		}
		
		return qtd;
	}
		
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "evento_calculaRetornos");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

		} catch (Exception e) {
			// aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}

}
