package xTestes;

import java.math.BigDecimal;

import com.sankhya.util.BigDecimalUtil;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.sql.NativeSql;

public class para_testar_skw implements AcaoRotinaJava{

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		//BigDecimal azul;
		//BigDecimal diasCorridos = BigDecimalUtil.getValueOrZero(NativeSql.getBigDecimal("CAST ((DBDATE() - DTNEG) AS INT)", "TGFCAB", "NUNOTA = ?", nota));
		//BigDecimal codUsuLogado = (azul != null) ? azul : BigDecimal.ZERO;
		
	}

}
