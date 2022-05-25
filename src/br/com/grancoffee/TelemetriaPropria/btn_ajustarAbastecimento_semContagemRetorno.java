package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.TimeUtils;

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
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btn_ajustarAbastecimento_semContagemRetorno implements AcaoRotinaJava{
	
	int t = 0;
	int a = 0;
	int n = 0;
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		
		for(int i=0; i < linhas.length; i++) {
			start(linhas[i]);
			t++;
		}
		
		arg0.setMensagemRetorno("<br/><b>ATENÇÃO !</b><br/><br/> Registro Selecionados: "+t+"\nRegistros ajustados (com 0 contagem e 0 retorno): "+a+"\nRegistros não ajustados: "+n);
		
	}
	
	private void start(Registro linhas) throws Exception {
		BigDecimal usuarioValidacao = (BigDecimal) linhas.getCampo("CODUSUVALIDACAO");
		BigDecimal id = (BigDecimal) linhas.getCampo("ID");
		String status = (String) linhas.getCampo("STATUS");
		
		if(usuarioValidacao == null && "3".equals(status)) {
			//TODO :: Contar a quantidade de teclas.
			BigDecimal qtdTeclas = BigDecimalUtil.getValueOrZero(descobreQtdTeclas(id));
			BigDecimal qtdContagem = BigDecimalUtil.getValueOrZero(descobreQtdContagem(id));
			BigDecimal qtdRetorno = BigDecimalUtil.getValueOrZero(descobreQtdRetorno(id));
			BigDecimal qtdDiferenca = BigDecimalUtil.getValueOrZero(descobreQtdDiferenca(id));
			
			if(qtdTeclas.intValue()>0) { //existem teclas
				if(qtdContagem.intValue()==0 && qtdDiferenca.intValue()==0) {
					
					if(qtdRetorno.intValue()==0) {
						ajustarTeclas(id);
					}else {
						pegarTeclas(id, TimeUtils.getNow());
						salvarNoHistorico(id);
					}
					
					linhas.setCampo("DTVALIDACAO", TimeUtils.getNow());
					linhas.setCampo("STATUSVALIDACAO", "2");
					linhas.setCampo("CODUSUVALIDACAO", getUsuLogado());
					a++;
				}else {
					n++;
				}
			}else {
				n++;
			}
		}else {
			n++;
		}
	}
	
	private BigDecimal descobreQtdTeclas(BigDecimal id) {
		BigDecimal qtd = BigDecimal.ZERO;
		
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT COUNT(*) FROM AD_ITENSRETABAST WHERE ID="+id);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("COUNT(*)");
				qtd = new BigDecimal(count);
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return qtd;
	}
	
	private BigDecimal descobreQtdContagem(BigDecimal id) {
		BigDecimal qtd = BigDecimal.ZERO;
		
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT COUNT(*) FROM AD_ITENSRETABAST WHERE ID="+id+" AND CONTAGEM > 0");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("COUNT(*)");
				qtd = new BigDecimal(count);
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return qtd;
	}
	
	private BigDecimal descobreQtdRetorno(BigDecimal id) {
		BigDecimal qtd = BigDecimal.ZERO;
		
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT COUNT(*) FROM AD_ITENSRETABAST WHERE ID="+id+" AND QTDRETORNO > 0");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("COUNT(*)");
				qtd = new BigDecimal(count);
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return qtd;
	}
	
	private BigDecimal descobreQtdDiferenca(BigDecimal id) {
		BigDecimal qtd = BigDecimal.ZERO;
		
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT COUNT(*) FROM AD_ITENSRETABAST WHERE ID="+id+" AND DIFERENCA > 0");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("COUNT(*)");
				qtd = new BigDecimal(count);
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return qtd;
	}
	
	private void ajustarTeclas(BigDecimal id) throws Exception {

		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("AD_ITENSRETABAST", "this.ID = ? ", new Object[] { id }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
						
				DynamicVO.setProperty("AJUSTADO", "S");
				itemEntity.setValueObject((EntityVO) DynamicVO);
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}

	}
	
	private void pegarTeclas(BigDecimal idObjeto, Timestamp hora) throws Exception {

		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		Collection<?> parceiro = dwfEntityFacade
				.findByDynamicFinder(new FinderWrapper("AD_ITENSRETABAST", "this.ID = ? ", new Object[] { idObjeto }));

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			
			BigDecimal saldoApos = DynamicVO.asBigDecimal("SALDOAPOS");
			BigDecimal saldoEsperado = DynamicVO.asBigDecimal("SALDOESPERADO"); 
			
			if(saldoApos.subtract(saldoEsperado).intValue()!=0) {
				String codbem = DynamicVO.asString("CODBEM");
				String tecla = DynamicVO.asString("TECLA");
				BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
				BigDecimal capacidade = DynamicVO.asBigDecimal("CAPACIDADE");
				BigDecimal nivelpar = DynamicVO.asBigDecimal("NIVELPAR");
				
				BigDecimal valor = saldoApos.subtract(saldoEsperado);
				
				if(codbem==null) {
					codbem = getCodbem(idObjeto);
				}
				
				inserirSolicitacaoDeAjuste(codbem, tecla, produto, capacidade, nivelpar, saldoEsperado, valor,
						idObjeto, hora);
			}
						
			DynamicVO.setProperty("AJUSTADO", "S");
			itemEntity.setValueObject((EntityVO) DynamicVO);
		}

	}
	
	private String getCodbem(Object idabast) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_RETABAST");
		DynamicVO VO = DAO.findOne("ID=?", new Object[] { idabast });
		String codbem = VO.asString("CODBEM");
		return codbem;
	}
	
	private void inserirSolicitacaoDeAjuste(String codbem, String tecla, BigDecimal produto, BigDecimal capacidade, BigDecimal nivelpar, 
			BigDecimal saldoAnterior, BigDecimal valor, Object idObjeto, Timestamp hora) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("GCSolicitAjuste");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("CODBEM", codbem);
			VO.setProperty("CODUSU", getUsuLogado());
			VO.setProperty("TECLA", tecla);
			VO.setProperty("CODPROD", produto);
			VO.setProperty("CAPACIDADE", capacidade);
			VO.setProperty("NIVELPAR", nivelpar);
			VO.setProperty("SALDOANTERIOR", saldoAnterior);
			VO.setProperty("QTDAJUSTE", valor);
			VO.setProperty("MANUAL", "N");
			VO.setProperty("OBSERVACAO", "Botão Ajustar Abastecimento Sem Contagem");
			VO.setProperty("SALDOFINAL", saldoAnterior.add(valor));
			VO.setProperty("IDABASTECIMENTO", idObjeto);
			VO.setProperty("AD_DTSOLICIT", hora);

			dwfFacade.createEntity("GCSolicitAjuste", (EntityVO) VO);

		} catch (Exception e) {
			//salvarException("[inserirSolicitacaoDeAjuste] Não foi possivel cadastrar a solicitação de ajuste!"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void salvarNoHistorico(BigDecimal idabast) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_HISTRET");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("IDRETORNO", idabast);
			VO.setProperty("DTSOLICI", TimeUtils.getNow());
			VO.setProperty("AJUSTADO", "N");
			
			dwfFacade.createEntity("AD_HISTRET", (EntityVO) VO);
		} catch (Exception e) {
			//salvarException("[salvarNoHistorico] nao foi possivel salvar no histórico! "+e.getCause()+"\n"+e.getMessage());
		}
	}
	
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
		codUsuLogado = ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID();
		return BigDecimalUtil.getValueOrZero(codUsuLogado);
	}

}
