package com.example.cramschool.service;

import java.util.Locale;

import org.springframework.stereotype.Component;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

@Component
public class UrlSlugSupport {

	private final HanyuPinyinOutputFormat pinyinFormat;

	public UrlSlugSupport() {
		this.pinyinFormat = new HanyuPinyinOutputFormat();
		this.pinyinFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
		this.pinyinFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		this.pinyinFormat.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
	}

	public String slugify(String value) {
		StringBuilder slug = new StringBuilder();
		boolean previousDash = false;
		for (int i = 0; i < value.length(); i++) {
			char character = Character.toLowerCase(value.charAt(i));
			if (isAsciiLetterOrDigit(character)) {
				slug.append(character);
				previousDash = false;
			} else if (!previousDash && !slug.isEmpty()) {
				slug.append('-');
				previousDash = true;
			}
		}
		int length = slug.length();
		if (length > 0 && slug.charAt(length - 1) == '-') {
			slug.deleteCharAt(length - 1);
		}
		return slug.toString().toLowerCase(Locale.ROOT);
	}

	public String pinyinSlug(String value) {
		return slugify(toPinyin(value));
	}

	public String normalizeBlank(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String toPinyin(String value) {
		StringBuilder pinyin = new StringBuilder();
		for (int i = 0; i < value.length(); i++) {
			char character = value.charAt(i);
			String[] syllables = pinyinFor(character);
			if (syllables != null && syllables.length > 0) {
				pinyin.append(' ').append(syllables[0]);
			} else {
				pinyin.append(character);
			}
		}
		return pinyin.toString();
	}

	private String[] pinyinFor(char character) {
		try {
			return PinyinHelper.toHanyuPinyinStringArray(character, pinyinFormat);
		} catch (BadHanyuPinyinOutputFormatCombination ex) {
			throw new IllegalStateException("無法轉換中文拼音", ex);
		}
	}

	private boolean isAsciiLetterOrDigit(char character) {
		return (character >= 'a' && character <= 'z') || (character >= '0' && character <= '9');
	}
}
