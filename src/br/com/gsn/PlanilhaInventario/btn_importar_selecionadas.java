package br.com.gsn.PlanilhaInventario;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btn_importar_selecionadas implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		
		for(int i=0; i<linhas.length; i++) {
			start(linhas[i]);
		}
		
	}
	
	public void start(Registro linhas) throws Exception {
		Timestamp data = (Timestamp) linhas.getCampo("DTCONTAGEM");
		BigDecimal empresa = (BigDecimal) linhas.getCampo("CODEMP");
		BigDecimal local = (BigDecimal) linhas.getCampo("CODLOCAL");
		BigDecimal produto = (BigDecimal) linhas.getCampo("CODPROD");
		String controle = (String) linhas.getCampo("CONTROLE");
		
		boolean validaSeExiste = validaSeExiste(data,empresa,local,produto,controle);
		
		if(validaSeExiste) {
			linhas.setCampo("OBSERVACAO", "Existe");
		}
	}
	
	public boolean validaSeExiste(Timestamp data,BigDecimal empresa,BigDecimal local,BigDecimal produto,String controle) {
		
		SimpleDateFormat formatador = new SimpleDateFormat("dd/MM/YYYY");
		Date dataX = new Date(data.getTime());
		String dataFormatada = formatador.format(dataX);
		
		boolean valida=false;
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT COUNT(*) FROM TGFCTE WHERE DTCONTAGEM='"+dataFormatada+"' AND CODEMP="+empresa+" AND CODLOCAL="+local+" AND CODPROD="+produto+" AND CONTROLE='"+controle+"' AND SEQUENCIA=1");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("COUNT(*)");
				if (count >= 1) {
					valida = true;
				}
			}

			
		} catch (Exception e) {
			System.out.println("############# Erro! "+e.getMessage()+"\n"+e.getCause());
		}
		
		return valida;
	}

}
